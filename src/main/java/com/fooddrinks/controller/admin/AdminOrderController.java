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

import com.fooddrinks.dto.response.AdminOrderDetailResponse;
import com.fooddrinks.dto.response.AdminOrderSummaryResponse;
import com.fooddrinks.entity.OrderStatus;
import com.fooddrinks.exception.BadRequestException;
import com.fooddrinks.exception.ResourceNotFoundException;
import com.fooddrinks.service.OrderService;
import com.fooddrinks.util.AdminPaths;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping(AdminPaths.Orders.URL)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final int PAGE_SIZE = 15;

    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AdminOrderSummaryResponse> orders = orderService.getAllOrdersForAdmin(
                PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending()));
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        return AdminPaths.Orders.VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            AdminOrderDetailResponse order = orderService.getOrderByIdForAdmin(id);
            model.addAttribute("order", order);
            model.addAttribute("allowedStatuses", orderService.getAllowedTransitions(order.getStatus()));
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + AdminPaths.Orders.URL;
        }
        return AdminPaths.Orders.VIEW_DETAIL;
    }

    /**
     * Update order status.
     * Uses POST (not GET) so the action is protected by CSRF.
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam OrderStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + id + " status updated to " + status + ".");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:" + AdminPaths.Orders.URL;
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + AdminPaths.Orders.URL + "/" + id;
    }
}
