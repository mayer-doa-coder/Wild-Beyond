package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import com.wildbeyond.service.ProductService;
import com.wildbeyond.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes authenticated users to the correct role-based dashboard view.
 *
 * Flow:
 *   GET /dashboard
 *     → Spring Security injects the current Authentication
 *     → We inspect the granted authorities
 *     → Return the matching Thymeleaf template path
 *
 * Role → Template mapping:
 *   ROLE_ADMIN  → templates/admin/dashboard.html
 *   ROLE_SELLER → templates/seller/dashboard.html
 *   (default)   → templates/buyer/dashboard.html
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"))) {
            return "redirect:/seller/dashboard";
        }

        return "redirect:/buyer/dashboard";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        model.addAttribute("userCount", userService.countUsers());
        model.addAttribute("productCount", productService.countProducts());
        model.addAttribute("orderCount", orderService.countOrders());
        return "admin/dashboard";
    }

    @GetMapping("/seller/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerDashboard(Model model) {
        model.addAttribute("myProducts", productService.countProductsByCurrentSeller());
        model.addAttribute("myOrders", orderService.countOrdersForSeller());
        model.addAttribute("myBuyingOrders", orderService.countBuyingOrdersForCurrentSeller());
        return "seller/dashboard";
    }

    @GetMapping("/buyer/dashboard")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerDashboard(Model model) {
        model.addAttribute("myOrders", orderService.countOrdersByCurrentUser());
        model.addAttribute("recentOrders", orderService.findMyOrders().stream().limit(5).toList());
        return "buyer/dashboard";
    }
}
