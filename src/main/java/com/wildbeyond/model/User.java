package com.wildbeyond.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    // ── Relationships ────────────────────────────────────────────────────────

    /**
     * User ↔ Role  (ManyToMany)
     * Owning side: User  |  join table: user_roles
     * LAZY – load roles only when explicitly accessed.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * User → Product  (OneToMany)
     * Inverse side – Product owns the FK (seller_id).
     * PERSIST + MERGE only: saving/updating a User propagates to their products.
     * No REMOVE / orphanRemoval – products are independent business records;
     * deleting a seller must be handled explicitly in the service layer.
     */
    @OneToMany(mappedBy = "seller", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    /**
     * User → Order  (OneToMany)
     * Inverse side – Order owns the FK (buyer_id).
     * No cascade: orders are independent business records.
     * LAZY is the @OneToMany default, declared explicitly for clarity.
     */
    @OneToMany(mappedBy = "buyer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}

