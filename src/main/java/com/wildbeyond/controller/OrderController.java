package com.wildbeyond.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller — stub. Requires authentication.
 * Any logged-in user (BUYER, SELLER, ADMIN) can access orders.
 */
@Controller
@RequestMapping("/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    @GetMapping("")
    public String getAllOrders() {
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    public String getOrderById(@PathVariable Long id) {
        return "redirect:/orders";
    }

    @PostMapping("")
    public String createOrder() {
        return "redirect:/orders";
    }
}