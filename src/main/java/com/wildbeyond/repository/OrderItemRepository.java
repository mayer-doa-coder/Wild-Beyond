package com.wildbeyond.repository;

import com.wildbeyond.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items belonging to a specific order.
     * Used for the order details page and total calculation.
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find all order items for a specific product.
     * Used for sales analytics and inventory tracking.
     */
    List<OrderItem> findByProductId(Long productId);
}
