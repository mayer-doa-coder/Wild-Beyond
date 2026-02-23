package com.wildbeyond.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    private BigDecimal totalAmount;

    private String shippingAddress;

    private String paymentMethod;

    private LocalDateTime orderedAt;

    private LocalDateTime updatedAt;
}
