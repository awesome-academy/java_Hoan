package com.fooddrinks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fooddrinks.service.impl.CustomOAuth2UserService;
import com.fooddrinks.service.impl.CustomOidcUserService;
import com.fooddrinks.util.AdminPaths;
import com.fooddrinks.util.ApiPaths;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final CustomOidcUserService customOidcUserService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    /**
     * Admin UI filter chain — session-based form login.
     * Matches only /admin/** so the API chain is unaffected.
     * CSRF is enabled (Thymeleaf th:action injects the token automatically).
     * Session fixation protection: changeSessionId on login.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AdminPaths.ROOT + "/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AdminPaths.Login.URL).permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage(AdminPaths.Login.URL)
                        .loginProcessingUrl(AdminPaths.Login.URL)
                        .defaultSuccessUrl(AdminPaths.Dashboard.URL, true)
                        .failureUrl(AdminPaths.Login.URL + "?error"))
                .logout(logout -> logout
                        .logoutUrl(AdminPaths.Login.LOGOUT_URL)
                        .logoutSuccessUrl(AdminPaths.Login.URL + "?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // Prevent session fixation: issue a new session ID on login
                        .sessionFixation()
                        .changeSessionId())
                .exceptionHandling(ex -> ex
                        // Non-admin authenticated users get 403 → redirect to login
                        .accessDeniedPage(AdminPaths.Login.URL + "?denied"));
        return http.build();
    }

    /**
     * API filter chain — stateless JWT + OAuth2 social login.
     *
     * Session policy: IF_REQUIRED allows Spring Security to create a temporary
     * session during the OAuth2 handshake (needed for state parameter storage).
     * After success, OAuth2SuccessHandler invalidates the session and issues a JWT;
     * all subsequent API calls are authenticated statelessly via JwtAuthenticationFilter.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // IF_REQUIRED: session created only during OAuth2 flow, not for JWT requests
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Public: product & category browsing (GET only) + static files
                        .requestMatchers(HttpMethod.GET, ApiPaths.Products.URL + "/**", ApiPaths.Categories.URL + "/**")
                        .permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Auth endpoints are public
                        .requestMatchers(ApiPaths.Auth.URL + "/**").permitAll()
                        // OAuth2 flow endpoints — Spring Security handles these internally
                        .requestMatchers(ApiPaths.OAuth2.AUTHORIZE + "/**",
                                         ApiPaths.OAuth2.CALLBACK + "/**").permitAll()
                        // OAuth2 post-login redirect target (receives ?token=... for testing)
                        .requestMatchers("/oauth2/callback").permitAll()
                        // Everything else requires a valid JWT or active OAuth2 session
                        .anyRequest().authenticated())
                // Return ApiResponse JSON for 401/403 instead of Spring's default HTML/empty
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // OAuth2 social login (Google, Facebook, Apple)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestResolver(
                                        new CustomOAuth2AuthorizationRequestResolver(
                                                clientRegistrationRepository)))
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService)   // Google + Apple (OIDC)
                                .userService(customOAuth2UserService))    // Facebook (OAuth2)
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                // Validate JWT before Spring Security's own filters
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
