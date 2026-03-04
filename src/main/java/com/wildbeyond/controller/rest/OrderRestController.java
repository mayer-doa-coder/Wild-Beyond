package com.wildbeyond.controller.rest;

import com.wildbeyond.dto.OrderDTO;
import com.wildbeyond.model.Order;
import com.wildbeyond.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Order management.
 *
 * Base path: /api/orders
 *
 * Authenticated users:      POST /api/orders       — place a new order
 *                           GET  /api/orders/my     — view own orders
 *                           GET  /api/orders/{id}   — view a specific order
 * ADMIN only:               GET  /api/orders        — view all orders
 *                           DELETE /api/orders/{id} — cancel / remove an order
 *
 * URL-level access rules are defined in SecurityConfig.
 * @PreAuthorize adds defence-in-depth.
 *
 * Security note: buyerId is NOT accepted from the request body.
 * The buyer is resolved from the authenticated principal inside OrderService.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;

    // ── Place order (any authenticated user) ──────────────────────────────────

    /**
     * POST /api/orders
     * Places a new order for the current authenticated buyer.
     * Only BUYER role is permitted — sellers sell, buyers buy.
     * Status is automatically set to PENDING.
     */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Order> create(@Valid @RequestBody OrderDTO dto) {
        Order saved = orderService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Read own orders (any authenticated user) ──────────────────────────────

    /**
     * GET /api/orders/my
     * Returns only the orders placed by the currently authenticated buyer.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getMyOrders() {
        return ResponseEntity.ok(orderService.findMyOrders());
    }

    /**
     * GET /api/orders/{id}
     * Returns a single order by id.
     * Returns 404 if the order does not exist.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    /**
     * GET /api/orders
     * Returns all orders in the system. ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAll() {
        return ResponseEntity.ok(orderService.findAll());
    }

    /**
     * DELETE /api/orders/{id}
     * Cancels / removes an order. ADMIN only.
     * Returns 204 NO CONTENT on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
