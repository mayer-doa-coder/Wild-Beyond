package com.wildbeyond.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
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

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "admin/dashboard";
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"))) {
            return "seller/dashboard";
        }

        return "buyer/dashboard";
    }
}
