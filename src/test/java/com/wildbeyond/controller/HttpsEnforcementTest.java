package com.wildbeyond.controller;

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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRestController.class)
@Import({SecurityConfig.class, RateLimitingFilter.class, HttpsEnforcementFilter.class})
@TestPropertySource(properties = {
        "app.security.require-https=true",
        "app.security.rate-limit.enabled=false"
})
class HttpsEnforcementTest {

    @Autowired
    private WebApplicationContext context;

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
    void insecureHttpRequest_redirectsToHttps() throws Exception {
        mockMvc.perform(get("/api/products").secure(false))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://**"));
    }
}
