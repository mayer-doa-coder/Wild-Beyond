package com.wildbeyond.dto;

import com.wildbeyond.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for updating order status.
 */
@Data
public class OrderStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
