package com.wildbeyond.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating or updating a product.
 * Used by ProductRestController and ProductController.
 *
 * Note on sellerId:
 *   - Required on CREATE (POST) — used to associate the product with a seller.
 *   - Required field on UPDATE (PUT) by the same DTO contract, but the service
 *     intentionally ignores it on update (the seller never changes after creation).
 */
@Data
public class ProductDTO {

    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be a positive value")
    private BigDecimal price;

    // @PositiveOrZero — stock=0 is valid (out-of-stock), only negative is rejected.
    // @Positive would wrongly reject a product reaching zero stock.
    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock must be zero or a positive value")
    private Integer stock;
}
