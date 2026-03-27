package com.wildbeyond.service;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.exception.UnauthorizedAccessException;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Product CRUD operations.
 *
 * All write operations are @Transactional.
 * Read operations use a read-only transaction for optimal performance.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + email));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ADMIN"::equals);
    }

    private void assertCanManageProduct(User actor, Product product) {
        if (isAdmin(actor)) {
            return;
        }

        Long ownerId = product.getSeller().getId();
        if (!actor.getId().equals(ownerId)) {
            throw new UnauthorizedAccessException("You do not own this resource");
        }
    }

    /**
     * Create a new product and associate it with an existing seller.
     *
     * @param dto validated product data including sellerId
     * @throws ResourceNotFoundException if no User exists with the given sellerId
     */
    @Transactional
    public Product create(ProductDTO dto) {
        User seller = userRepository.findById(dto.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller not found with id: " + dto.getSellerId()));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setSeller(seller);

        return productRepository.save(product);
    }

    /**
     * Return all products. Safe for the public product listing page.
     */
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Return a single product by id.
     *
     * @throws ResourceNotFoundException if no Product exists with the given id
     */
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    /**
     * Return all products belonging to a specific seller.
     *
     * @param sellerId the ID of the seller
     */
    @Transactional(readOnly = true)
    public List<Product> findBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    /**
     * Update the mutable fields of an existing product.
     * The seller is intentionally not re-assigned on update.
     *
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional
    public Product update(Long id, ProductDTO dto) {
        Product product = findById(id);
        User actor = currentUser();
        assertCanManageProduct(actor, product);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        return productRepository.save(product);
    }

    /**
     * Delete a product by id.
     *
     * @throws ResourceNotFoundException if no Product exists with the given id,
     *         so the controller returns 404 instead of a silent 204.
     */
    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        User actor = currentUser();
        assertCanManageProduct(actor, product);

        productRepository.deleteById(id);
    }
}
