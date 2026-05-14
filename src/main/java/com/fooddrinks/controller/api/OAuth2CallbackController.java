package com.fooddrinks.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Temporary test endpoint for OAuth2 callback.
 * Renders the oauth2-callback.html page; the JWT token is read from the
 * URL fragment (#token=...) by client-side JavaScript — it is never sent
 * to or processed by the server.
 *
 * This controller is only needed when app.oauth2.redirect-uri points to
 * localhost:8080 for local testing. In production it points to the frontend.
 */
@Controller
@RequestMapping("/oauth2/callback")
public class OAuth2CallbackController {

    @GetMapping
    public String callback(
            @RequestParam(required = false) String error,
            Model model) {

        if (error != null) {
            model.addAttribute("error", error);
        }
        return "oauth2-callback";
    }
}
