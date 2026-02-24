package com.wildbeyond.repository;

import com.wildbeyond.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find a role by its name (ADMIN, SELLER, BUYER).
     * Used when assigning roles to a newly registered user.
     */
    Optional<Role> findByName(String name);
}
