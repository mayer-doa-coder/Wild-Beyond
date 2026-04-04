package com.wildbeyond.controller;

import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.dto.OrderDTO;
import com.wildbeyond.dto.OrderItemDTO;
import com.wildbeyond.model.Order;
import com.wildbeyond.model.OrderStatus;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getOrders_shouldUseFindMyOrders_forNonAdmin() throws Exception {
        Order myOrder = Order.builder()
                .id(1L)
                .orderDate(LocalDateTime.of(2026, 1, 1, 10, 0))
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        when(orderService.findMyOrders()).thenReturn(List.of(myOrder));

        mockMvc.perform(get("/orders")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/orders/1")));

        verify(orderService).findMyOrders();
    }

    @Test
    void getOrders_shouldUseFindAll_forAdmin() throws Exception {
        Order order = Order.builder()
                .id(2L)
                .orderDate(LocalDateTime.of(2026, 1, 2, 12, 0))
                .status(OrderStatus.DELIVERED)
                .totalPrice(BigDecimal.valueOf(80.00))
                .build();

        when(orderService.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/orders")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));

        verify(orderService).findAll();
    }

        @Test
        void getOrders_shouldUseSellerProductOrders_whenSellerViewSelling() throws Exception {
                Order order = Order.builder()
                                .id(3L)
                                .orderDate(LocalDateTime.of(2026, 1, 3, 14, 0))
                                .status(OrderStatus.PENDING)
                                .totalPrice(BigDecimal.valueOf(120.00))
                                .build();

                when(orderService.findOrdersForCurrentSellerProducts()).thenReturn(List.of(order));

                mockMvc.perform(get("/orders")
                                                .param("view", "selling")
                                                .with(user("seller@example.com").roles("SELLER")))
                                .andExpect(status().isOk())
                                .andExpect(view().name("orders"))
                                .andExpect(model().attribute("ordersView", "selling"));

                verify(orderService).findOrdersForCurrentSellerProducts();
        }

    @Test
    void getOrderById_shouldRenderDetailView_whenAuthorized() throws Exception {
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(10L);
        item.setProductName("Hiking Boots");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(100.00));

        OrderDTO dto = new OrderDTO();
        dto.setId(5L);
        dto.setOrderDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        dto.setStatus("PENDING");
        dto.setTotalPrice(BigDecimal.valueOf(200.00));
        dto.setItems(List.of(item));

        when(orderService.getOrderById(5L)).thenReturn(dto);

        mockMvc.perform(get("/orders/5")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(view().name("order-detail"))
                .andExpect(model().attributeExists("order"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Order Details")));
    }

    @Test
    void getOrderById_shouldReturn403_whenForbidden() throws Exception {
        when(orderService.getOrderById(5L)).thenThrow(new AccessDeniedException("forbidden"));

        mockMvc.perform(get("/orders/5")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderById_shouldReturn404_whenMissing() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new RuntimeException("missing"));

        mockMvc.perform(get("/orders/999")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().isNotFound());
    }

        @Test
        void updateOrderStatus_shouldRedirectToOrderDetail() throws Exception {
                when(orderService.updateStatus(5L, "CANCELLED")).thenReturn(new Order());

                mockMvc.perform(post("/orders/5/status")
                                                .with(user("buyer@example.com").roles("BUYER"))
                                                .with(csrf())
                                                .param("status", "CANCELLED"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/orders/5"));

                verify(orderService).updateStatus(5L, "CANCELLED");
        }
}
