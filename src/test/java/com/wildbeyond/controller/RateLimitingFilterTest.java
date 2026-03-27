package com.wildbeyond.controller;

import tools.jackson.databind.ObjectMapper;
import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.controller.rest.ProductRestController;
import com.wildbeyond.model.Product;
import com.wildbeyond.security.HttpsEnforcementFilter;
import com.wildbeyond.security.RateLimitingFilter;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRestController.class)
@Import({SecurityConfig.class, RateLimitingFilter.class, HttpsEnforcementFilter.class})
@TestPropertySource(properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.api.capacity=2",
        "app.security.rate-limit.api.refill-minutes=1",
        "app.security.rate-limit.login.capacity=5",
        "app.security.rate-limit.login.refill-minutes=1",
        "app.security.require-https=false"
})
class RateLimitingFilterTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Product product = Product.builder()
                .id(10L)
                .name("Tent")
                .description("A sturdy tent")
                .price(BigDecimal.valueOf(199.99))
                .stock(10)
                .build();

        when(productService.findAll()).thenReturn(List.of(product));
    }

    @Test
    void apiRateLimit_returns429_whenThresholdExceeded() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"));
    }

    @Test
    void loginRateLimit_returns429_afterFiveAttempts() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .param("email", "unknown@example.com")
                            .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection());
        }

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .param("email", "unknown@example.com")
                        .param("password", "wrong"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"));
    }
}
