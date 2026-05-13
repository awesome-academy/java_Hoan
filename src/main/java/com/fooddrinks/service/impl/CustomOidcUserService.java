package com.fooddrinks.service.impl;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.User;
import com.fooddrinks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Handles OIDC login for Google and Apple Sign In.
 *
 * Key Apple-specific behaviour:
 *   - On FIRST login: Apple includes email (real or relay) and optionally name in the id_token.
 *   - On SUBSEQUENT logins: Apple omits email and name; only {@code sub} (providerId) is reliable.
 *     → We look up by provider+providerId first so repeat logins always succeed.
 *
 * Security: same provider-conflict check as OAuth2; account-disabled check included.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final CustomOAuth2UserService oauth2UserService; // shared processUser logic

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = oauth2UserService.resolveProvider(registrationId);

        // sub (subject) is always present and unique per provider
        String providerId = oidcUser.getSubject();
        // email is null for Apple on subsequent logins
        String email      = oidcUser.getEmail();
        String name       = oidcUser.getFullName();
        String picture    = oidcUser.getPicture();

        processOidcUser(email, providerId, name, picture, provider);

        return oidcUser;
    }

    private void processOidcUser(String email, String providerId, String name,
                                  String picture, Provider provider) {
        // Lookup by providerId first — critical for Apple repeat logins where email is absent
        Optional<User> byProviderId = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProviderId.isPresent()) {
            User user = byProviderId.get();
            if (!user.getIsActive()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_disabled", "Your account has been disabled.", null));
            }
            // Refresh name/avatar if provider sends updated info
            if (name != null)    user.setFullName(name);
            if (picture != null) user.setAvatarUrl(picture);
            userRepository.save(user);
            return;
        }

        // On Apple first login, email must be present to create the account
        if (email == null) {
            // No user found by providerId AND no email — cannot identify the user
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "user_not_found",
                    "Account not found. If this is your first Apple Sign In, "
                    + "please ensure you have granted email access.",
                    null));
        }

        // Delegate email-based lookup + conflict check + creation to shared logic
        oauth2UserService.processUser(email, providerId, name, picture, provider);
    }
}
