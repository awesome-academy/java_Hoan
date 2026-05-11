package com.fooddrinks.controller.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin login page.
 * POST /admin/login is handled by Spring Security's formLogin() — no controller method needed.
 */
@Controller
public class AdminLoginController {

    /** Redirect root /admin to /admin/users */
    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/login")
    public String loginPage(@AuthenticationPrincipal UserDetails userDetails) {
        // If already authenticated, skip the login page
        if (userDetails != null) {
            return "redirect:/admin/users";
        }
        return "admin/login";
    }
}
