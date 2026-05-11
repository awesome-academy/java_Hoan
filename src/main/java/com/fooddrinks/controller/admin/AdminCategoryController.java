package com.fooddrinks.controller.admin;

import com.fooddrinks.dto.request.CategoryRequest;
import com.fooddrinks.dto.response.CategoryResponse;
import com.fooddrinks.exception.ConflictException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        List<CategoryResponse> categories = categoryService.getAll();
        model.addAttribute("categories", categories);
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new CategoryRequest());
        model.addAttribute("formAction", "/admin/categories");
        model.addAttribute("pageTitle", "New Category");
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("category") CategoryRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/admin/categories");
            model.addAttribute("pageTitle", "New Category");
            return "admin/categories/form";
        }
        try {
            categoryService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
        } catch (ConflictException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("formAction", "/admin/categories");
            model.addAttribute("pageTitle", "New Category");
            return "admin/categories/form";
        }
        return "redirect:/admin/categories";
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
            model.addAttribute("categoryId", id);
            model.addAttribute("formAction", "/admin/categories/" + id);
            model.addAttribute("pageTitle", "Edit Category");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
        return "admin/categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("category") CategoryRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("formAction", "/admin/categories/" + id);
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        }
        try {
            categoryService.update(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
        } catch (ConflictException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categoryId", id);
            model.addAttribute("formAction", "/admin/categories/" + id);
            model.addAttribute("pageTitle", "Edit Category");
            return "admin/categories/form";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
