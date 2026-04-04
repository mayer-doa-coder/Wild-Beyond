package com.wildbeyond.repository;

import com.wildbeyond.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products listed by a specific seller.
     * Used for the seller dashboard and seller-scoped product filtering.
     */
    List<Product> findBySellerId(Long sellerId);

    long countBySellerId(Long sellerId);

    List<Product> findTop5ByOrderByIdDesc();
}
