package com.wildbeyond.repository;

import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders placed by a specific buyer.
     * Used for buyer order history and admin tracking.
     */
    List<Order> findByBuyerId(Long buyerId);

        long countByBuyerId(Long buyerId);

        @Query("""
            select count(distinct o.id)
            from Order o
            join o.items i
            where i.product.seller.id = :sellerId
            """)
        long countOrdersContainingSellerProducts(@Param("sellerId") Long sellerId);

    /**
     * Find all orders with a specific status.
     * Used for admin order management (e.g. filter PENDING orders).
     */
    List<Order> findByStatus(OrderStatus status);
}
