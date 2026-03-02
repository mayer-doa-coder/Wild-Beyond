package com.wildbeyond.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller — stub. Redirects to products until order views are built.
 */
@Controller
@RequestMapping("/orders")
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