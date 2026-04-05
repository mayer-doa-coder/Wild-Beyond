package com.wildbeyond.repository;

import com.wildbeyond.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = "seller")
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = "seller")
    Optional<Product> findById(Long id);

    /**
     * Find all products listed by a specific seller.
     * Used for the seller dashboard and seller-scoped product filtering.
     */
    List<Product> findBySellerId(Long sellerId);

    long countBySellerId(Long sellerId);

    List<Product> findTop5ByOrderByIdDesc();
}
