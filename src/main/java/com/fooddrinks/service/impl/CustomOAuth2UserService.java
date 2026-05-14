package com.fooddrinks.service.impl;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.Role;
import com.fooddrinks.entity.User;
import com.fooddrinks.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles OAuth2 login for non-OIDC providers (currently Facebook).
 *
 * Responsibilities:
 * 1. Load user attributes from provider's userinfo endpoint.
 * 2. Find or create the local User record.
 * 3. Reject login if the email belongs to a different provider (no account
 * linking).
 *
 * Security: transactions are scoped tightly; no sensitive data is logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = resolveProvider(registrationId);

        // Log only non-sensitive attribute keys (not values) for diagnostics
        log.debug("[OAuth2] Provider={} | emailPresent={} | attributeKeys={}",
                provider, oauth2User.getAttribute("email") != null, oauth2User.getAttributes().keySet());

        // Facebook uses 'id' as the unique user identifier (getName() returns it)
        String providerId = oauth2User.getName();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = extractPictureUrl(oauth2User);

        processUser(email, providerId, name, picture, provider);

        return oauth2User;
    }

    void processUser(String email, String providerId, String name,
            String picture, Provider provider) {
        // Check by providerId first — most reliable, avoids stale email matches
        Optional<User> byProviderId = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProviderId.isPresent()) {
            User user = byProviderId.get();
            if (!user.getIsActive()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_disabled", "Your account has been disabled.", null));
            }
            if (name != null)
                user.setFullName(name);
            if (picture != null)
                user.setAvatarUrl(picture);
            userRepository.save(user);
            return;
        }

        // Check by email — handles the "same email, re-login" scenario
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                if (existing.getProvider() != provider) {
                    // Same email registered with a different provider → reject
                    throw new OAuth2AuthenticationException(new OAuth2Error(
                            "email_conflict",
                            "This email is already registered with " + existing.getProvider().name()
                                    + ". Please sign in using that method.",
                            null));
                }
                if (!existing.getIsActive()) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("account_disabled", "Your account has been disabled.", null));
                }
                // Same provider, providerId not yet stored — back-fill it
                existing.setProviderId(providerId);
                if (name != null)
                    existing.setFullName(name);
                if (picture != null)
                    existing.setAvatarUrl(picture);
                userRepository.save(existing);
                return;
            }
        }

        // Cannot create account without an email
        if (email == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "missing_email",
                    "Cannot sign in: " + provider.name() + " did not provide an email address. "
                            + "Please grant email access in your " + provider.name() + " account settings.",
                    null));
        }

        // New user — create account automatically
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(name != null ? name : "User");
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setEmailVerified(true);
        newUser.setIsActive(true);
        newUser.setRole(Role.USER);
        userRepository.save(newUser);
    }

    private String extractPictureUrl(OAuth2User user) {
        // Facebook returns picture as a nested object; extract URL if available
        Object picture = user.getAttribute("picture");
        if (picture instanceof String s)
            return s;
        // Facebook nested: {"data": {"url": "..."}}
        if (picture instanceof java.util.Map<?, ?> map) {
            Object data = map.get("data");
            if (data instanceof java.util.Map<?, ?> dataMap) {
                Object url = dataMap.get("url");
                return url instanceof String s ? s : null;
            }
        }
        return null;
    }

    Provider resolveProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> Provider.GOOGLE;
            case "facebook" -> Provider.FACEBOOK;
            case "apple" -> Provider.APPLE;
            default -> throw new OAuth2AuthenticationException(new OAuth2Error(
                    "unknown_provider", "Unknown OAuth2 provider: " + registrationId, null));
        };
    }
}
