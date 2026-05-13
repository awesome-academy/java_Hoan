package com.fooddrinks.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Registers OAuth2/OIDC clients for Google, Facebook, and Apple.
 *
 * Defining this bean manually overrides Spring Boot's auto-configuration, which
 * is necessary to inject Apple's dynamically-generated JWT client_secret at startup.
 *
 * CommonOAuth2Provider was removed in Spring Security 7; registrations are built
 * explicitly using ClientRegistrations.fromOidcIssuerLocation() for OIDC providers
 * (Google) and ClientRegistration.withRegistrationId() for others (Facebook, Apple).
 *
 * Google  — OIDC discovery via accounts.google.com
 * Facebook — OAuth2 (non-OIDC); user-info URI includes explicit fields for email
 * Apple   — OIDC with custom endpoints; client_secret is an ES256 JWT
 */
@Configuration
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            AppleClientSecretGenerator appleSecretGenerator) {

        // Google: OIDC — endpoints discovered from the well-known configuration URI.
        // ClientRegistrations.fromOidcIssuerLocation ensures we always use current endpoints.
        ClientRegistration google = ClientRegistrations
                .fromOidcIssuerLocation("https://accounts.google.com")
                .registrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .scope("openid", "email", "profile")
                .clientName("Google")
                .build();

        // Facebook: non-OIDC — manual endpoint configuration.
        // user-info URI explicitly requests the fields we need (email is not returned by default).
        ClientRegistration facebook = ClientRegistration.withRegistrationId("facebook")
                .clientId(facebookClientId)
                .clientSecret(facebookClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email,picture")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();

        // Apple: OIDC with JWT client authentication.
        // client_secret is generated at startup by AppleClientSecretGenerator.
        // response_mode=form_post is added by CustomOAuth2AuthorizationRequestResolver.
        ClientRegistration apple = ClientRegistration.withRegistrationId("apple")
                .clientId(appleClientId)
                .clientSecret(appleSecretGenerator.generate())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "name")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .issuerUri("https://appleid.apple.com")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Apple")
                .build();

        return new InMemoryClientRegistrationRepository(google, facebook, apple);
    }
}


