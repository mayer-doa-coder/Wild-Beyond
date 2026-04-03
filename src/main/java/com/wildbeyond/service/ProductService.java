package com.wildbeyond.service;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
     * Return a DTO for edit form binding in MVC flows.
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = findById(id);
        ProductDTO dto = new ProductDTO();
        dto.setSellerId(product.getSeller().getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        return dto;
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

    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public long countProductsByCurrentSeller() {
        User seller = currentUser();
        return productRepository.countBySellerId(seller.getId());
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
        assertCanManageProduct(product);
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
        assertCanManageProduct(product);
        productRepository.delete(product);
    }

    /**
     * Explicit MVC-oriented update method name for controller readability.
     */
    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        return update(id, dto);
    }

    /**
     * Explicit MVC-oriented delete method name for controller readability.
     */
    @Transactional
    public void deleteProduct(Long id) {
        delete(id);
    }

    @Transactional(readOnly = true)
    public boolean canCurrentUserManage(Long productId) {
        Product product = findById(productId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }

        return userRepository.findByEmail(authentication.getName())
                .map(user -> product.getSeller().getId().equals(user.getId()))
                .orElse(false);
    }

    private void assertCanManageProduct(Product product) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required to manage products");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            return;
        }

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current user not found: " + authentication.getName()));

        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not allowed to manage this product");
        }
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Current user not found: " + authentication.getName()));
    }
}
