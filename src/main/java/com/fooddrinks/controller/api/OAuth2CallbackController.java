package com.fooddrinks.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fooddrinks.util.JwtUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * Temporary test endpoint for OAuth2 callback.
 * Receives the JWT token after a successful social login and renders it
 * via Thymeleaf so it can be copied for Postman testing.
 *
 * This controller is only needed when app.oauth2.redirect-uri points to
 * localhost:8080 for local testing. In production it points to the frontend.
 */
@Controller
@RequestMapping("/oauth2/callback")
@RequiredArgsConstructor
public class OAuth2CallbackController {

    private final JwtUtil jwtUtil;

    @GetMapping
    public String callback(
            @RequestParam(required = false) String error,
            HttpSession session,
            Model model) {

        if (error != null) {
            model.addAttribute("error", error);
            return "oauth2-callback";
        }

        // Read token from session and immediately remove it so it cannot be replayed
        String token = (String) session.getAttribute("oauth2_token");
        if (token != null) {
            session.removeAttribute("oauth2_token");
            // Invalidate the rest of the OAuth2 session — all subsequent requests use JWT
            session.invalidate();
            String email = jwtUtil.tryExtractEmail(token).orElse("(invalid token)");
            model.addAttribute("token", token);
            model.addAttribute("email", email);
        }

        return "oauth2-callback";
    }
}
