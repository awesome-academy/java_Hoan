package com.fooddrinks.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin login page.
 * POST /admin/login is handled by Spring Security's formLogin() — no controller
 * method needed.
 */
@Controller
public class AdminLoginController {

    /** Redirect root /admin to /admin/dashboard */
    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/login")
    public String loginPage(@AuthenticationPrincipal UserDetails userDetails) {
        // Only redirect if the user is actually an ADMIN — non-admin authenticated
        // users
        // would otherwise trigger a redirect loop (/admin/login → /admin/users → 403 →
        // /admin/login)
        if (userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/users";
        }
        return "admin/login";
    }
}
