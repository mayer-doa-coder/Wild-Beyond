package com.wildbeyond.repository;

import com.wildbeyond.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique email address.
     * Used by Spring Security for authentication (Day-3).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check whether a user with the given email already exists.
     * Used during registration to prevent duplicates.
     */
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    List<User> findAllByOrderByIdAsc();
}
