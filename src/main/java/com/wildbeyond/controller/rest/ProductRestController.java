package com.wildbeyond.controller.rest;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.model.Product;
import com.wildbeyond.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Product management.
 *
 * Base path: /api/products
 *
 * Public:                 GET  /api/products
 *                         GET  /api/products/{id}
 * SELLER or ADMIN:        POST /api/products
 *                         PUT  /api/products/{id}
 *                         DELETE /api/products/{id}
 *
 * URL-level access rules are defined in SecurityConfig.
 * @PreAuthorize adds defence-in-depth.
 *
 * Note: This controller is intentionally separate from the Thymeleaf
 * ProductController (/products). Both controllers can coexist safely.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    // ── Read (Public) ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    // ── Write (SELLER / ADMIN only) ───────────────────────────────────────────

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductDTO dto) {
        Product saved = productService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id,
                                          @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
