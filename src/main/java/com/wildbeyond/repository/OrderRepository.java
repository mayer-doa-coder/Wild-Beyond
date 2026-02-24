package com.wildbeyond.repository;

import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders placed by a specific buyer.
     * Used for buyer order history and admin tracking.
     */
    List<Order> findByBuyerId(Long buyerId);

    /**
     * Find all orders with a specific status.
     * Used for admin order management (e.g. filter PENDING orders).
     */
    List<Order> findByStatus(OrderStatus status);
}
