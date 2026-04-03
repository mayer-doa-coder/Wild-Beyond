package com.wildbeyond.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO representing a single line item when placing an order.
 * Used as part of OrderDTO.
 */
@Data
public class OrderItemDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be at least 1")
    private Integer quantity;

    // Populated for read/detail views.
    private String productName;
    private BigDecimal unitPrice;
}
