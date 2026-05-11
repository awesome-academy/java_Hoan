package com.fooddrinks.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds common model attributes for all admin controllers.
 * Used to expose request URI for sidebar active-state highlighting
 * without relying on #request (removed in Thymeleaf 3.1).
 */
@ControllerAdvice(basePackages = "com.fooddrinks.controller.admin")
public class AdminModelAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
