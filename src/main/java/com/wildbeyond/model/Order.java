package com.wildbeyond.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    // ── Relationships ────────────────────────────────────────────────────────

    /**
     * Order → User (Buyer)  (ManyToOne)
     * Owning side – holds the FK column buyer_id.
     * LAZY – do not pull entire User when loading an Order.
     *
     * @JsonIgnore prevents circular serialization: Order → User.orders → [Order...]
     * Buyer identity is not exposed in REST responses; resolve from /api/users/{id}.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /**
     * Order → OrderItem  (OneToMany)
     * Inverse side – OrderItem owns the FK (order_id).
     * CascadeType.ALL + orphanRemoval: items live and die with the Order.
     *
     * EAGER – items are always serialized with the order in REST responses.
     * LAZY would cause LazyInitializationException after the transaction closes
     * because open-in-view is disabled (spring.jpa.open-in-view=false).
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ── Audit Lifecycle ──────────────────────────────────────────────────────

    /**
     * Called by Hibernate before the first INSERT.
     * Sets orderDate to now if the caller did not supply one explicitly.
     * The check guards against overwriting a deliberately supplied date
     * (e.g. historical order imports from an external system).
     */
    @PrePersist
    private void prePersist() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
    }
}
