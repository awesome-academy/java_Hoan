package com.fooddrinks.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fooddrinks.service.DashboardService;
import com.fooddrinks.util.AdminPaths;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping(AdminPaths.Dashboard.URL)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return AdminPaths.Dashboard.VIEW;
    }
}
