package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Order views (Thymeleaf MVC).
 * Requires authentication — any logged-in user (BUYER, SELLER, ADMIN).
 *
 * REST operations live in controller/rest/OrderRestController.
 */
@Controller
@RequestMapping("/orders")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("")
    public String getMyOrders(Model model) {
        model.addAttribute("orders", orderService.findMyOrders());
        return "orders";
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