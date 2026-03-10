package com.wildbeyond.controller;

import tools.jackson.databind.ObjectMapper;
import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.controller.rest.OrderRestController;
import com.wildbeyond.dto.OrderDTO;
import com.wildbeyond.dto.OrderItemDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderStatus;
import com.wildbeyond.model.User;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for OrderRestController using MockMvc.
 *
 * @WebMvcTest loads only the web layer (controllers, filters, security).
 * Services are replaced with Mockito mocks via @MockitoBean.
 * CustomUserDetailsService is mocked because SecurityConfig depends on it.
 *
 * Role simulation is done with @WithMockUser(roles = "...").
 * Write operations include .with(csrf()) to satisfy CSRF protection.
 */
@WebMvcTest(OrderRestController.class)
@Import(SecurityConfig.class)
class OrderRestControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // Required by SecurityConfig — not used in assertions
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Order order;
    private OrderDTO dto;

    @BeforeEach
    void setUp() {
        // Build MockMvc with springSecurity() so @WithMockUser propagates
        // through the Spring Security filter chain during test execution.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        User buyer = new User();
        buyer.setId(1L);
        buyer.setEmail("buyer@example.com");

        order = new Order();
        order.setId(5L);
        order.setBuyer(buyer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(200.00));

        OrderItemDTO itemDto = new OrderItemDTO();
        itemDto.setProductId(10L);
        itemDto.setQuantity(2);

        dto = new OrderDTO();
        dto.setItems(List.of(itemDto));
    }

    // ── POST /api/orders (BUYER / ADMIN) ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "BUYER")
    void create_returns201_whenBuyerPlacesOrder() throws Exception {
        when(orderService.create(any(OrderDTO.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void create_returns201_whenAdminPlacesOrder() throws Exception {
        when(orderService.create(any(OrderDTO.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void create_returns403_whenSellerTriesToPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/orders/my (authenticated) ───────────────────────────────────

    @Test
    @WithMockUser(roles = "BUYER")
    void getMyOrders_returns200_forAuthenticatedBuyer() throws Exception {
        when(orderService.findMyOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void getMyOrders_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/orders/{id} (authenticated) ─────────────────────────────────

    @Test
    @WithMockUser(roles = "BUYER")
    void getById_returns200_whenOrderExists() throws Exception {
        when(orderService.findById(5L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void getById_returns404_whenOrderMissing() throws Exception {
        when(orderService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found with id: 99"));
    }

    @Test
    void getById_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/orders (ADMIN only) ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_returns200_forAdmin() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void getAll_returns403_forBuyer() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).findAll();
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void getAll_returns403_forSeller() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/orders/{id} (ADMIN only) ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204_forAdmin() throws Exception {
        doNothing().when(orderService).delete(5L);

        mockMvc.perform(delete("/api/orders/5").with(csrf()))
                .andExpect(status().isNoContent());

        verify(orderService).delete(5L);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void delete_returns403_forBuyer() throws Exception {
        mockMvc.perform(delete("/api/orders/5").with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns404_whenOrderMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found with id: 99"))
                .when(orderService).delete(99L);

        mockMvc.perform(delete("/api/orders/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found with id: 99"));
    }
}
