package com.fooddrinks.controller.admin;

import com.fooddrinks.dto.request.ProductRequest;
import com.fooddrinks.dto.response.ProductResponse;
import com.fooddrinks.entity.ProductType;
import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.CategoryService;
import com.fooddrinks.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private static final int PAGE_SIZE = 15;

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<ProductResponse> products = productService.getAllForAdmin(
                PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending()));
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("productTypes", ProductType.values());
        model.addAttribute("formAction", "/admin/products");
        model.addAttribute("pageTitle", "New Product");
        return "admin/products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") ProductRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAll());
            model.addAttribute("productTypes", ProductType.values());
            model.addAttribute("formAction", "/admin/products");
            model.addAttribute("pageTitle", "New Product");
            return "admin/products/form";
        }
        try {
            ProductResponse created = productService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully.");
            return "redirect:/admin/products/" + created.getId() + "/edit";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.getAll());
            model.addAttribute("productTypes", ProductType.values());
            model.addAttribute("formAction", "/admin/products");
            model.addAttribute("pageTitle", "New Product");
            return "admin/products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            ProductResponse existing = productService.getByIdForAdmin(id);
            ProductRequest form = new ProductRequest();
            form.setName(existing.getName());
            form.setDescription(existing.getDescription());
            form.setPrice(existing.getPrice());
            form.setType(existing.getType());
            form.setStockQuantity(existing.getStockQuantity());
            form.setCategoryId(existing.getCategoryId());
            model.addAttribute("product", form);
            model.addAttribute("productDetail", existing);
            model.addAttribute("categories", categoryService.getAll());
            model.addAttribute("productTypes", ProductType.values());
            model.addAttribute("productId", id);
            model.addAttribute("formAction", "/admin/products/" + id);
            model.addAttribute("pageTitle", "Edit Product");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
        return "admin/products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("product") ProductRequest request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("productDetail", productService.getByIdForAdmin(id));
            } catch (ResourceNotFoundException ignored) { }
            model.addAttribute("categories", categoryService.getAll());
            model.addAttribute("productTypes", ProductType.values());
            model.addAttribute("productId", id);
            model.addAttribute("formAction", "/admin/products/" + id);
            model.addAttribute("pageTitle", "Edit Product");
            return "admin/products/form";
        }
        try {
            productService.updateForAdmin(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deactivated.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.restoreProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product restored.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    /**
     * Upload a product image.
     * isPrimary defaults to false; pass ?primary=true to set as primary.
     */
    @PostMapping("/{id}/images")
    public String uploadImage(@PathVariable Long id,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam(defaultValue = "false") boolean primary,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.addImage(id, file, primary);
            redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully.");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id,
                               @PathVariable Long imageId,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.deleteImage(id, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted.");
        } catch (ResourceNotFoundException | BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + id + "/edit";
    }
}
