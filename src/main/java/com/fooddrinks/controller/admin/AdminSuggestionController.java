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

import com.fooddrinks.dto.response.AdminSuggestionResponse;
import com.fooddrinks.entity.SuggestionStatus;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.SuggestionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/suggestions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSuggestionController {

    private static final int PAGE_SIZE = 15;

    private final SuggestionService suggestionService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AdminSuggestionResponse> suggestions = suggestionService.getAllSuggestionsForAdmin(
                PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending()));
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("currentPage", page);
        model.addAttribute("allStatuses", SuggestionStatus.values());
        return "admin/suggestions/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            AdminSuggestionResponse suggestion = suggestionService.getSuggestionByIdForAdmin(id);
            model.addAttribute("suggestion", suggestion);
            model.addAttribute("allStatuses", SuggestionStatus.values());
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/suggestions";
        }
        return "admin/suggestions/detail";
    }

    /**
     * Update suggestion status.
     * Uses POST (not GET) so the action is protected by CSRF.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam SuggestionStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            suggestionService.updateSuggestionStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Suggestion #" + id + " status updated to " + status + ".");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/suggestions/" + id;
    }
}
