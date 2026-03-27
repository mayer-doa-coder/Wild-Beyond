package com.wildbeyond.service;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.exception.UnauthorizedAccessException;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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

    @InjectMocks
    private ProductService productService;

    private User seller;
    private Product product;
    private ProductDTO dto;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(1L);
        seller.setName("Seller One");
        seller.setEmail("seller@example.com");
        seller.setRoles(Set.of(Role.builder().name("SELLER").build()));

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
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
        mockSecurityContext("seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));

        ProductDTO updateDto = new ProductDTO();
        updateDto.setSellerId(1L);
        updateDto.setName("Pro Tent");
        updateDto.setDescription("Heavy-duty shelter");
        updateDto.setPrice(BigDecimal.valueOf(349.99));
        updateDto.setStock(25);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
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
    void update_throwsResourceNotFound_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

        @Test
        void update_throwsUnauthorized_whenSellerDoesNotOwnProduct() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmail("other@example.com");
        otherSeller.setRoles(Set.of(Role.builder().name("SELLER").build()));

        Product otherProduct = Product.builder()
            .id(11L)
            .name("Boots")
            .description("All-weather boots")
            .price(BigDecimal.valueOf(99.99))
            .stock(5)
            .seller(otherSeller)
            .build();

        mockSecurityContext("seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.findById(11L)).thenReturn(Optional.of(otherProduct));

        assertThatThrownBy(() -> productService.update(11L, dto))
            .isInstanceOf(UnauthorizedAccessException.class)
            .hasMessage("You do not own this resource");

        verify(productRepository, never()).save(any());
        }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void delete_callsRepository_withCorrectId() {
        mockSecurityContext("seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById(10L);

        productService.delete(10L);

        verify(productRepository).deleteById(10L);
    }

    @Test
    void delete_throwsUnauthorized_whenSellerDoesNotOwnProduct() {
        User otherSeller = new User();
        otherSeller.setId(2L);
        otherSeller.setEmail("other@example.com");
        otherSeller.setRoles(Set.of(Role.builder().name("SELLER").build()));

        Product otherProduct = Product.builder()
                .id(11L)
                .name("Boots")
                .description("All-weather boots")
                .price(BigDecimal.valueOf(99.99))
                .stock(5)
                .seller(otherSeller)
                .build();

        mockSecurityContext("seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.findById(11L)).thenReturn(Optional.of(otherProduct));

        assertThatThrownBy(() -> productService.delete(11L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You do not own this resource");

        verify(productRepository, never()).deleteById(any());
    }
}
