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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

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
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/users", true)
                        .failureUrl("/admin/login?error"))
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // Prevent session fixation: issue a new session ID on login
                        .sessionFixation()
                        .changeSessionId())
                .exceptionHandling(ex -> ex
                        // Non-admin authenticated users get 403 → redirect to login
                        .accessDeniedPage("/admin/login?denied"));
        return http.build();
    }

    /**
     * API filter chain — stateless JWT.
     * Handles /api/** and all other routes not matched by adminFilterChain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: product & category browsing (GET only) + static files
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // Auth endpoints are public
                        .requestMatchers("/api/auth/**").permitAll()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated())
                // Return ApiResponse JSON for 401/403 instead of Spring's default HTML/empty
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Validate JWT before Spring Security's own filters
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
