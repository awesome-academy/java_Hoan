package com.fooddrinks.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fooddrinks.dto.request.CategoryRequest;
import com.fooddrinks.dto.response.CategoryResponse;
import com.fooddrinks.exception.ConflictException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.CategoryService;
import com.fooddrinks.util.AdminPaths;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping(AdminPaths.Categories.URL)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        List<CategoryResponse> categories = categoryService.getAll();
        model.addAttribute("categories", categories);
        return AdminPaths.Categories.VIEW_LIST;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new CategoryRequest());
        populateNewForm(model);
        return AdminPaths.Categories.VIEW_FORM;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("category") CategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateNewForm(model);
            return AdminPaths.Categories.VIEW_FORM;
        }
        try {
            categoryService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
        } catch (ConflictException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateNewForm(model);
            return AdminPaths.Categories.VIEW_FORM;
        }
        return "redirect:" + AdminPaths.Categories.URL;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            CategoryResponse existing = categoryService.getById(id);
            CategoryRequest form = new CategoryRequest();
            form.setName(existing.getName());
            form.setDescription(existing.getDescription());
            model.addAttribute("category", form);
            populateEditForm(model, id);
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + AdminPaths.Categories.URL;
        }
        return AdminPaths.Categories.VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("category") CategoryRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditForm(model, id);
            return AdminPaths.Categories.VIEW_FORM;
        }
        try {
            categoryService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
        } catch (ConflictException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateEditForm(model, id);
            return AdminPaths.Categories.VIEW_FORM;
        }
        return "redirect:" + AdminPaths.Categories.URL;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + AdminPaths.Categories.URL;
    }

    // --- helpers ---

    private void populateNewForm(Model model) {
        model.addAttribute("formAction", AdminPaths.Categories.URL);
        model.addAttribute("pageTitle", "New Category");
    }

    private void populateEditForm(Model model, Long id) {
        model.addAttribute("categoryId", id);
        model.addAttribute("formAction", AdminPaths.Categories.URL + "/" + id);
        model.addAttribute("pageTitle", "Edit Category");
    }
}
