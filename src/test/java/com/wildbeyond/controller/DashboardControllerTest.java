package com.wildbeyond.controller;

import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.OrderService;
import com.wildbeyond.service.ProductService;
import com.wildbeyond.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void dashboard_shouldRedirectAdminToAdminRoute() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void dashboard_shouldRedirectSellerToSellerRoute() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("seller@example.com").roles("SELLER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/dashboard"));
    }

    @Test
    void dashboard_shouldRedirectBuyerToBuyerRoute() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/dashboard"));
    }

    @Test
    void adminDashboard_shouldRenderGlobalCountsForAdmin() throws Exception {
        when(userService.countUsers()).thenReturn(12L);
        when(productService.countProducts()).thenReturn(30L);
        when(orderService.countOrders()).thenReturn(9L);

        mockMvc.perform(get("/admin")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("userCount", 12L))
                .andExpect(model().attribute("productCount", 30L))
                .andExpect(model().attribute("orderCount", 9L));

        verify(userService).countUsers();
        verify(productService).countProducts();
        verify(orderService).countOrders();
    }

    @Test
    void sellerDashboard_shouldRenderSellerMetricsForSeller() throws Exception {
        when(productService.countProductsByCurrentSeller()).thenReturn(7L);
        when(orderService.countOrdersForSeller()).thenReturn(4L);

        mockMvc.perform(get("/seller/dashboard")
                        .with(user("seller@example.com").roles("SELLER")))
                .andExpect(status().isOk())
                .andExpect(view().name("seller/dashboard"))
                .andExpect(model().attribute("myProducts", 7L))
                .andExpect(model().attribute("myOrders", 4L));

        verify(productService).countProductsByCurrentSeller();
        verify(orderService).countOrdersForSeller();
    }

    @Test
    void buyerDashboard_shouldRenderBuyerMetricsForBuyer() throws Exception {
        when(orderService.countOrdersByCurrentUser()).thenReturn(5L);

        mockMvc.perform(get("/buyer/dashboard")
                        .with(user("buyer@example.com").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/dashboard"))
                .andExpect(model().attribute("myOrders", 5L));

        verify(orderService).countOrdersByCurrentUser();
    }

    @Test
    void adminDashboard_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/admin")
                        .with(user("seller@example.com").roles("SELLER")))
                .andExpect(status().isForbidden());
    }
}
