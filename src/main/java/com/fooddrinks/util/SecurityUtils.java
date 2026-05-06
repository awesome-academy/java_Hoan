package com.fooddrinks.util;

import com.fooddrinks.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Helper to extract the current authenticated user's email from SecurityContext.
 * Used in Day 4+ (Cart, Order, Rating, Suggestion) to identify who is making the request.
 *
 * Usage:
 *   String email = SecurityUtils.getCurrentUserEmail();
 */
public class SecurityUtils {

    private SecurityUtils() {}

    /** Returns the email (username) of the currently authenticated user. */
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new UnauthorizedException("Not authenticated");
    }
}
