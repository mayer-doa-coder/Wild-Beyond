package com.wildbeyond.service;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderItem;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.OrderItemRepository;
import com.wildbeyond.repository.OrderRepository;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService.
 *
 * Uses Mockito only — no Spring context loaded, no database required.
 * Covers: create, findAll, findById, update, delete.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProductService productService;

    private User seller;
    private Product product;
    private ProductDTO dto;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        seller = new User();
        seller.setId(1L);
        seller.setName("Seller One");
        seller.setEmail("seller@example.com");

        product = Product.builder()
                .id(10L)
                .name("Tent")
                .description("A sturdy tent")
                .price(BigDecimal.valueOf(199.99))
                .stock(50)
                .seller(seller)
                .build();

        dto = new ProductDTO();
        dto.setSellerId(1L);
        dto.setName("Tent");
        dto.setDescription("A sturdy tent");
        dto.setPrice(BigDecimal.valueOf(199.99));
        dto.setStock(50);
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_savesProduct_whenSellerExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Tent");
        assertThat(result.getSeller()).isEqualTo(seller);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_snapshotsAllFields_fromDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        productService.create(dto);

        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getDescription()).isEqualTo("A sturdy tent");
        assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
        assertThat(saved.getStock()).isEqualTo(50);
    }

    @Test
    void create_throwsResourceNotFound_whenSellerMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seller not found with id: 1");

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_throwsAccessDenied_whenSellerTriesToCreateForAnotherSeller() {
        setAuthenticatedUser("seller@example.com", "ROLE_SELLER");

        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmail("other@example.com");

        ProductDTO otherSellerDto = new ProductDTO();
        otherSellerDto.setSellerId(2L);
        otherSellerDto.setName("Tent");
        otherSellerDto.setDescription("A sturdy tent");
        otherSellerDto.setPrice(BigDecimal.valueOf(199.99));
        otherSellerDto.setStock(50);

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherSeller));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> productService.create(otherSellerDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("create products for themselves");

        verify(productRepository, never()).save(any());
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(1).contains(product);
    }

    @Test
    void findAll_returnsEmptyList_whenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertThat(productService.findAll()).isEmpty();
    }

    @Test
    void countProducts_returnsRepositoryCount() {
        when(productRepository.count()).thenReturn(42L);

        assertThat(productService.countProducts()).isEqualTo(42L);
    }

    @Test
    void countProductsByCurrentSeller_returnsSellerScopedCount() {
        setAuthenticatedUser("seller@example.com", "ROLE_SELLER");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.countBySellerId(1L)).thenReturn(6L);

        assertThat(productService.countProductsByCurrentSeller()).isEqualTo(6L);
    }

    // ── findById ────────────────────────────────────────────────────────────

    @Test
    void findById_returnsProduct_whenExists() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        Product result = productService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Tent");
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void update_updatesFields_andSaves() {
        setAuthenticatedUser("seller@example.com", "ROLE_SELLER");

        ProductDTO updateDto = new ProductDTO();
        updateDto.setSellerId(1L);
        updateDto.setName("Pro Tent");
        updateDto.setDescription("Heavy-duty shelter");
        updateDto.setPrice(BigDecimal.valueOf(349.99));
        updateDto.setStock(25);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(10L, updateDto);

        assertThat(result.getName()).isEqualTo("Pro Tent");
        assertThat(result.getDescription()).isEqualTo("Heavy-duty shelter");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(349.99));
        assertThat(result.getStock()).isEqualTo(25);
        // Seller must NOT change on update
        assertThat(result.getSeller()).isEqualTo(seller);
    }

    @Test
    void update_throwsAccessDenied_whenSellerDoesNotOwnProduct() {
        setAuthenticatedUser("other@example.com", "ROLE_SELLER");

        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmail("other@example.com");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherSeller));

        assertThatThrownBy(() -> productService.update(10L, dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");

        verify(productRepository, never()).save(any());
    }

    @Test
    void update_allowsAdminOverride() {
        setAuthenticatedUser("admin@example.com", "ROLE_ADMIN");

        ProductDTO updateDto = new ProductDTO();
        updateDto.setSellerId(999L);
        updateDto.setName("Admin Updated Tent");
        updateDto.setDescription("Admin update");
        updateDto.setPrice(BigDecimal.valueOf(399.99));
        updateDto.setStock(10);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(10L, updateDto);

        assertThat(result.getName()).isEqualTo("Admin Updated Tent");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_throwsResourceNotFound_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void delete_callsRepository_withCorrectId() {
        setAuthenticatedUser("seller@example.com", "ROLE_SELLER");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByProductId(10L)).thenReturn(List.of());

        productService.delete(10L);

        verify(orderItemRepository).findByProductId(10L);
        verify(productRepository).delete(product);
    }

        @Test
        void delete_recalculatesOrderTotal_whenProductIsReferencedInOrders() {
        setAuthenticatedUser("seller@example.com", "ROLE_SELLER");

        Product otherProduct = Product.builder()
            .id(99L)
            .name("Binocular")
            .price(BigDecimal.valueOf(50.00))
            .stock(10)
            .seller(seller)
            .build();

        Order order = new Order();
        order.setId(91L);
        order.setItems(new ArrayList<>());

        OrderItem removable = OrderItem.builder()
            .id(1L)
            .order(order)
            .product(product)
            .quantity(1)
            .unitPrice(BigDecimal.valueOf(199.99))
            .build();

        OrderItem retained = OrderItem.builder()
            .id(2L)
            .order(order)
            .product(otherProduct)
            .quantity(2)
            .unitPrice(BigDecimal.valueOf(50.00))
            .build();

        order.getItems().add(removable);
        order.getItems().add(retained);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findByProductId(10L)).thenReturn(List.of(removable));

        productService.delete(10L);

        verify(orderRepository).save(order);
        assertThat(order.getItems()).containsExactly(retained);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("100.00");
        verify(productRepository).delete(product);
        }

    @Test
    void delete_throwsAccessDenied_whenSellerDoesNotOwnProduct() {
        setAuthenticatedUser("other@example.com", "ROLE_SELLER");

        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmail("other@example.com");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherSeller));

        assertThatThrownBy(() -> productService.delete(10L))
                .isInstanceOf(AccessDeniedException.class);

        verify(productRepository, never()).delete(any());
    }

    private void setAuthenticatedUser(String email, String role) {
        var authorities = Set.of(new SimpleGrantedAuthority(role));
        var authentication = new UsernamePasswordAuthenticationToken(email, "n/a", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
