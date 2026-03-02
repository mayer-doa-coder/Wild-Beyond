package com.wildbeyond.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Serves product-related Thymeleaf views.
 * Write operations are stubs — will be wired to ProductService in a future iteration.
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

    @PostMapping("")
    public String createProduct() {
        return "redirect:/products";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id) {
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        return "redirect:/products";
    }
}