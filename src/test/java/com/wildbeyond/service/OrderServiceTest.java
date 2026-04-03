package com.wildbeyond.service;

import com.wildbeyond.dto.OrderDTO;
import com.wildbeyond.dto.OrderItemDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.*;
import com.wildbeyond.repository.OrderRepository;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 *
 * Uses Mockito only — no Spring context or database required.
 * The Spring Security context is manually set up per test and torn down after.
 * Covers: create, findAll, findMyOrders, findById, delete.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private User seller;
    private User admin;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setId(2L);
        seller.setEmail("seller@example.com");
        seller.setRoles(Set.of(Role.builder().name("SELLER").build()));

        buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@example.com");
        buyer.setRoles(Set.of(Role.builder().name("BUYER").build()));

        admin = new User();
        admin.setId(99L);
        admin.setEmail("admin@example.com");
        admin.setRoles(Set.of(Role.builder().name("ADMIN").build()));

        product = Product.builder()
                .id(10L)
                .name("Hiking Boots")
                .price(BigDecimal.valueOf(100.00))
                .stock(20)
                .seller(seller)
                .build();

        order = new Order();
        order.setId(5L);
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(200.00));
        order.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(100.00));
        order.setItems(List.of(item));
    }

    @AfterEach
    void tearDown() {
        // Clear security context so tests don't bleed into each other
        SecurityContextHolder.clearContext();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Puts a fake Authentication into the SecurityContextHolder so that
     * OrderService.currentUser() can resolve the email principal.
     */
    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_savesOrder_forAuthenticatedBuyer() {
        mockSecurityContext("buyer@example.com");
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));

        OrderItemDTO itemDto = new OrderItemDTO();
        itemDto.setProductId(10L);
        itemDto.setQuantity(2);

        OrderDTO dto = new OrderDTO();
        dto.setItems(List.of(itemDto));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(5L);
            return o;
        });

        Order result = orderService.create(dto);

        assertThat(result.getBuyer()).isEqualTo(buyer);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        // 2 × £100.00 = £200.00
        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void create_throwsResourceNotFound_whenProductMissing() {
        mockSecurityContext("buyer@example.com");
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));

        OrderItemDTO itemDto = new OrderItemDTO();
        itemDto.setProductId(99L);
        itemDto.setQuantity(1);

        OrderDTO dto = new OrderDTO();
        dto.setItems(List.of(itemDto));

        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_throwsResourceNotFound_whenAuthenticatedUserMissing() {
        mockSecurityContext("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        OrderDTO dto = new OrderDTO();
        dto.setItems(List.of(new OrderItemDTO()));

        assertThatThrownBy(() -> orderService.create(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<Order> result = orderService.findAll();

        assertThat(result).hasSize(1).contains(order);
    }

    @Test
    void countOrders_returnsRepositoryCount() {
        when(orderRepository.count()).thenReturn(11L);

        assertThat(orderService.countOrders()).isEqualTo(11L);
    }

    @Test
    void countOrdersByCurrentUser_returnsBuyerScopedCount() {
        mockSecurityContext("buyer@example.com");
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
        when(orderRepository.countByBuyerId(1L)).thenReturn(3L);

        assertThat(orderService.countOrdersByCurrentUser()).isEqualTo(3L);
    }

    @Test
    void countOrdersForSeller_returnsSellerScopedCount() {
        mockSecurityContext("seller@example.com");
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(seller));
        when(orderRepository.countOrdersContainingSellerProducts(2L)).thenReturn(4L);

        assertThat(orderService.countOrdersForSeller()).isEqualTo(4L);
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_returnsOrder_whenExists() {
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        Order result = orderService.findById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 99");
    }

    // ── getOrderById (ownership-aware detail) ──────────────────────────────

    @Test
    void getOrderById_returnsMappedDto_forOwner() {
        mockSecurityContext("buyer@example.com");
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Hiking Boots");
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getOrderById_throwsAccessDenied_forNonOwner() {
        User otherBuyer = new User();
        otherBuyer.setId(7L);
        otherBuyer.setEmail("other@example.com");
        otherBuyer.setRoles(Set.of(Role.builder().name("BUYER").build()));

        mockSecurityContext("other@example.com");
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherBuyer));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void getOrderById_allowsAdmin_forAnyOrder() {
        mockSecurityContext("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getItems()).hasSize(1);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_deletesOrder_whenExists() {
        when(orderRepository.existsById(5L)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(5L);

        assertThatNoException().isThrownBy(() -> orderService.delete(5L));

        verify(orderRepository).deleteById(5L);
    }

    @Test
    void delete_throwsResourceNotFound_whenOrderMissing() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 99");

        verify(orderRepository, never()).deleteById(any());
    }
}
