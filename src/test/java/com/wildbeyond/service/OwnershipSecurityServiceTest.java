package com.wildbeyond.service;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.UnauthorizedAccessException;
import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderStatus;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.OrderRepository;
import com.wildbeyond.repository.ProductRepository;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration-style ownership tests using Spring context + @WithMockUser.
 * Repository beans are mocked so tests stay focused on service-level authorization logic.
 */
@SpringBootTest
class OwnershipSecurityServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private UserRepository userRepository;

    private ProductDTO updateDto;

    @BeforeEach
    void setUp() {
        updateDto = new ProductDTO();
        updateDto.setSellerId(1L);
        updateDto.setName("Updated Tent");
        updateDto.setDescription("Updated description");
        updateDto.setPrice(BigDecimal.valueOf(250.00));
        updateDto.setStock(12);
    }

    private static User user(Long id, String email, String roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRoles(Set.of(Role.builder().name(roleName).build()));
        return user;
    }

    @Test
    @WithMockUser(username = "seller1@example.com", roles = "SELLER")
    void sellerUpdatingOwnProduct_success() {
        User seller = user(1L, "seller1@example.com", "SELLER");

        Product product = Product.builder()
                .id(10L)
                .name("Tent")
                .description("A sturdy tent")
                .price(BigDecimal.valueOf(199.99))
                .stock(8)
                .seller(seller)
                .build();

        when(userRepository.findByEmail("seller1@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = productService.update(10L, updateDto);

        assertThat(updated.getName()).isEqualTo("Updated Tent");
        assertThat(updated.getSeller().getId()).isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "seller1@example.com", roles = "SELLER")
    void sellerUpdatingAnotherSellersProduct_fail() {
        User seller = user(1L, "seller1@example.com", "SELLER");
        User otherSeller = user(2L, "seller2@example.com", "SELLER");

        Product foreignProduct = Product.builder()
                .id(11L)
                .name("Foreign Product")
                .description("Owned by another seller")
                .price(BigDecimal.valueOf(300.00))
                .stock(3)
                .seller(otherSeller)
                .build();

        when(userRepository.findByEmail("seller1@example.com")).thenReturn(Optional.of(seller));
        when(productRepository.findById(11L)).thenReturn(Optional.of(foreignProduct));

        assertThatThrownBy(() -> productService.update(11L, updateDto))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You do not own this resource");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @WithMockUser(username = "buyer1@example.com", roles = "BUYER")
    void buyerAccessingOwnOrder_success() {
        User buyer = user(100L, "buyer1@example.com", "BUYER");

        Order ownOrder = new Order();
        ownOrder.setId(20L);
        ownOrder.setBuyer(buyer);
        ownOrder.setStatus(OrderStatus.PENDING);

        when(userRepository.findByEmail("buyer1@example.com")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(ownOrder));

        Order result = orderService.findById(20L);

        assertThat(result.getId()).isEqualTo(20L);
    }

    @Test
    @WithMockUser(username = "buyer1@example.com", roles = "BUYER")
    void buyerAccessingAnotherBuyersOrder_fail() {
        User buyer = user(100L, "buyer1@example.com", "BUYER");
        User otherBuyer = user(101L, "buyer2@example.com", "BUYER");

        Order foreignOrder = new Order();
        foreignOrder.setId(21L);
        foreignOrder.setBuyer(otherBuyer);
        foreignOrder.setStatus(OrderStatus.PENDING);

        when(userRepository.findByEmail("buyer1@example.com")).thenReturn(Optional.of(buyer));
        when(orderRepository.findById(21L)).thenReturn(Optional.of(foreignOrder));

        assertThatThrownBy(() -> orderService.findById(21L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You do not own this resource");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminAccessingAll_success() {
        User admin = user(999L, "admin@example.com", "ADMIN");
        User otherSeller = user(2L, "seller2@example.com", "SELLER");
        User otherBuyer = user(101L, "buyer2@example.com", "BUYER");

        Product foreignProduct = Product.builder()
                .id(30L)
                .name("Foreign Product")
                .description("Owned by seller2")
                .price(BigDecimal.valueOf(400.00))
                .stock(4)
                .seller(otherSeller)
                .build();

        Order foreignOrder = new Order();
        foreignOrder.setId(31L);
        foreignOrder.setBuyer(otherBuyer);
        foreignOrder.setStatus(OrderStatus.PENDING);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(productRepository.findById(30L)).thenReturn(Optional.of(foreignProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(31L)).thenReturn(Optional.of(foreignOrder));

        Product updated = productService.update(30L, updateDto);
        Order order = orderService.findById(31L);

        assertThat(updated.getName()).isEqualTo("Updated Tent");
        assertThat(order.getId()).isEqualTo(31L);
    }
}
