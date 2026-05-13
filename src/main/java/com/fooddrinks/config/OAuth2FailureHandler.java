package com.fooddrinks.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles OAuth2 login failures by redirecting to the frontend with a
 * user-friendly error message.
 *
 * Error codes from CustomOAuth2UserService / CustomOidcUserService are
 * mapped to human-readable strings. Internal details are never exposed.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String errorMessage = resolveErrorMessage(exception);
        log.warn("OAuth2 authentication failure: {}", exception.getMessage());

        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        String targetUrl = frontendRedirectUri + "?error=" + encodedError;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveErrorMessage(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            return switch (oauthEx.getError().getErrorCode()) {
                case "email_conflict"  ->
                        "This email is already registered with a different sign-in method. "
                        + "Please use the original sign-in method for this account.";
                case "missing_email"   ->
                        "Unable to retrieve your email from the provider. "
                        + "Please grant email access and try again.";
                case "account_disabled" ->
                        "Your account has been disabled. Please contact support.";
                case "user_not_found"  ->
                        "Account not found. Please sign up or grant email access.";
                default -> "Sign-in failed. Please try again.";
            };
        }
        return "Authentication failed. Please try again.";
    }
}
