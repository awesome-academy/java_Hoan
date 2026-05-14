package com.fooddrinks.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fooddrinks.dto.response.UserResponse;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.UserService;
import com.fooddrinks.util.AdminPaths;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping(AdminPaths.Users.URL)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int PAGE_SIZE = 15;

    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<UserResponse> users = userService.getAllUsers(
                PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending()));
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        return AdminPaths.Users.VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("user", userService.getUserById(id));
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + AdminPaths.Users.URL;
        }
        return AdminPaths.Users.VIEW_DETAIL;
    }

    /**
     * Toggle a user's isActive status.
     * Uses POST (not GET) so the action is protected by CSRF.
     */
    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            UserResponse user = userService.toggleActive(id);
            String status = Boolean.TRUE.equals(user.getIsActive()) ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + user.getEmail() + " has been " + status + ".");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + AdminPaths.Users.URL;
    }
}
