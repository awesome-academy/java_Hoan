package com.fooddrinks.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Customizes the OAuth2 authorization request for Apple Sign In.
 *
 * Apple requires {@code response_mode=form_post} for the authorization
 * endpoint,
 * meaning the callback from Apple is a POST request (not a GET redirect).
 * Spring Security's {@code OAuth2LoginAuthenticationFilter} handles POST
 * callbacks,
 * so no additional filter configuration is needed beyond this resolver.
 */
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        return customizeForApple(req, extractRegistrationId(request.getRequestURI()));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
        return customizeForApple(req, clientRegistrationId);
    }

    private OAuth2AuthorizationRequest customizeForApple(
            OAuth2AuthorizationRequest req,
            String registrationId) {
        if (req == null || !"apple".equals(registrationId)) {
            return req;
        }
        // Add response_mode=form_post without losing existing additional parameters
        Map<String, Object> params = new LinkedHashMap<>(req.getAdditionalParameters());
        params.put("response_mode", "form_post");
        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(params)
                .build();
    }

    /**
     * Extracts the last path segment (registrationId) from a URI like
     * /oauth2/authorization/apple
     */
    private String extractRegistrationId(String requestUri) {
        if (requestUri == null || requestUri.isBlank())
            return null;
        String[] segments = requestUri.split("/");
        return segments.length > 0 ? segments[segments.length - 1] : null;
    }
}
