package com.fooddrinks.config;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.repository.UserRepository;
import com.fooddrinks.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Issues a JWT and redirects to the configured frontend callback URI after
 * a successful OAuth2 / OIDC login.
 *
 * Token delivery: stored in the HTTP session under {@code "oauth2_token"} key,
 * then retrieved and cleared by the callback controller. The token never appears
 * in the URL, avoiding exposure in browser history and server access logs.
 *
 * In production, {@code app.oauth2.redirect-uri} should point to the real frontend;
 * the frontend reads the token from the session via a dedicated exchange endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String email = resolveEmail(authentication);

        // Gate on account status — user service already checks, but double-check here
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.getIsActive()) {
                throw new IllegalStateException("Account is disabled: " + email);
            }
        });

        String token = jwtUtil.generateToken(email);

        log.debug("OAuth2 login success — storing token in session for user: {}", email);

        // Store the token in session so the callback page can display it
        // without exposing it in the URL (browser history / server logs).
        // The callback controller will read and immediately remove it.
        HttpSession session = request.getSession(true);
        session.setAttribute("oauth2_token", token);

        getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri);
    }

    /**
     * Extracts the authenticated user's email from the OAuth2 principal.
     *
     * For Apple OIDC on subsequent logins the id_token omits the email claim,
     * so we fall back to a DB lookup by provider + providerId.
     */
    private String resolveEmail(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new IllegalStateException(
                    "Unexpected authentication type: " + authentication.getClass().getName());
        }

        Object principal = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null) return email;

            // Apple subsequent login: email not in id_token — look up by sub + provider
            String providerId = oidcUser.getSubject();
            Provider provider = resolveProvider(registrationId);
            return userRepository.findByProviderAndProviderId(provider, providerId)
                    .map(u -> u.getEmail())
                    .orElseThrow(() -> new IllegalStateException(
                            "No user found for provider=" + provider + ", providerId=" + providerId));
        }

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null) return email;
            throw new IllegalStateException(
                    "Email not available from OAuth2 provider: " + registrationId);
        }

        throw new IllegalStateException("Unknown principal type: " + principal.getClass().getName());
    }

    private Provider resolveProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google"   -> Provider.GOOGLE;
            case "facebook" -> Provider.FACEBOOK;
            case "apple"    -> Provider.APPLE;
            default -> throw new IllegalStateException("Unknown provider: " + registrationId);
        };
    }
}
