package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    public String getOrders(@RequestParam(value = "view", required = false) String view,
                            Model model,
                            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSeller = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));

        if (isAdmin) {
            model.addAttribute("orders", orderService.findAll());
            model.addAttribute("ordersView", "all");
            return "orders";
        }

        if (isSeller && "selling".equalsIgnoreCase(view)) {
            model.addAttribute("orders", orderService.findOrdersForCurrentSellerProducts());
            model.addAttribute("ordersView", "selling");
            return "orders";
        }

        model.addAttribute("orders", orderService.findMyOrders());
        model.addAttribute("ordersView", "buying");
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrderById(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("order", orderService.getOrderById(id));
            var allowedStatuses = orderService.getAllowedStatusUpdates(id);
            if (allowedStatuses == null) {
                allowedStatuses = List.of();
            }
            model.addAttribute("allowedStatuses", allowedStatuses);
            model.addAttribute("canUpdateStatus", !allowedStatuses.isEmpty());
            return "order-detail";
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("orderUpdateSuccess", "Order status updated.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("orderUpdateError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("orderUpdateError", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("")
    public String createOrder() {
        return "redirect:/orders";
    }
}