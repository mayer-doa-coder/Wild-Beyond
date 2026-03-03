package com.wildbeyond.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product views.
 * GET endpoints are public (browsing). Write operations require SELLER or ADMIN.
 * URL-level rules are in SecurityConfig; @PreAuthorize adds defense-in-depth.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    @GetMapping("")
    public String getAllProducts(Model model) {
        model.addAttribute("products", List.of());
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id) {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("")
    public String createProduct() {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id) {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        return "redirect:/products";
    }
}