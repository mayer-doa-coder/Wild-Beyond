package com.wildbeyond.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Values: ADMIN, SELLER, BUYER
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String name;
}
