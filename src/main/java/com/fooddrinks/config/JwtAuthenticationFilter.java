package com.fooddrinks.config;

import com.fooddrinks.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Intercepts every request and validates the JWT from the Authorization header.
 *
 * Flow:
 * 1. Extract "Bearer <token>" from Authorization header
 * 2. Validate token with JwtUtil
 * 3. Load UserDetails from DB
 * 4. Set authentication in SecurityContext
 *
 * Skips if header is missing or token is invalid (downstream security rules
 * handle access denial).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Parse token once — invalid/expired tokens return empty
        Optional<String> emailOpt = jwtUtil.tryExtractEmail(token);
        if (emailOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only set auth if not already authenticated in this request
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = emailOpt.get();

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(email);
            } catch (RuntimeException e) {
                // User deleted or DB error — treat token as invalid
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Reject tokens for disabled/locked/expired accounts
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()
                    || !userDetails.isAccountNonExpired() || !userDetails.isCredentialsNonExpired()) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
