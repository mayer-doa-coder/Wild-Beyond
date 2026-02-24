package com.wildbeyond.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * DTO for placing a new order.
 *
 * Security note: buyerId is intentionally absent. The buyer identity must be
 * resolved from the authenticated principal in the service layer, never trusted
 * from client input. Accepting buyerId from the request body would allow any
 * authenticated user to place orders on behalf of another user.
 */
@Data
public class OrderDTO {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDTO> items;
}
