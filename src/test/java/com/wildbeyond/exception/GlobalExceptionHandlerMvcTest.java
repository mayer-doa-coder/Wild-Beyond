package com.wildbeyond.exception;

import tools.jackson.databind.ObjectMapper;
import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.controller.rest.ProductRestController;
import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.ProductService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc-level tests for GlobalExceptionHandler.
 *
 * These complement GlobalExceptionHandlerTest (pure unit test) by verifying
 * that the handler is wired correctly into the controller-dispatcher pipeline
 * and that HTTP status codes and JSON error bodies are correct end-to-end.
 *
 * Scenarios:
 *   1. Invalid ID (unknown product) → ResourceNotFoundException → 404 + JSON error
 *   2. Validation failure (empty body) → MethodArgumentNotValidException → 400 + field errors
 *   3. Runtime fallback (service throws RuntimeException) → 400 + JSON error
 *   4. Validation: negative price → 400 with specific field message
 *   5. Validation: missing required fields selectively → 400 with correct keys
 */
@WebMvcTest(ProductRestController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerMvcTest {

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
    }

    // ── 1. Invalid ID → 404 ───────────────────────────────────────────────────

    @Test
    void getProductById_returns404_withJsonError_whenIdNotFound() throws Exception {
        when(productService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Product not found with id: 999"));
    }

    // ── 2. Validation failure (empty body) → 400 ─────────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void postProduct_returns400_withFieldErrors_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // All four required fields must report errors
                .andExpect(jsonPath("$.name").value("Product name is required"))
                .andExpect(jsonPath("$.price").value("Price is required"))
                .andExpect(jsonPath("$.stock").value("Stock quantity is required"))
                .andExpect(jsonPath("$.sellerId").value("Seller ID is required"));

        verify(productService, never()).create(any());
    }

    // ── 3. Runtime fallback → 400 ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void postProduct_returns400_withRuntimeError_whenServiceThrows() throws Exception {
        when(productService.create(any(ProductDTO.class)))
                .thenThrow(new RuntimeException("Seller not found with id: 7"));

        ProductDTO validDto = new ProductDTO();
        validDto.setSellerId(7L);
        validDto.setName("Tent");
        validDto.setDescription("Outdoor tent");
        validDto.setPrice(BigDecimal.valueOf(150.00));
        validDto.setStock(10);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Seller not found with id: 7"));
    }

    // ── 4. Validation: negative price → 400 ──────────────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void postProduct_returns400_withPriceError_whenPriceIsNegative() throws Exception {
        ProductDTO badPrice = new ProductDTO();
        badPrice.setSellerId(1L);
        badPrice.setName("Tent");
        badPrice.setPrice(BigDecimal.valueOf(-10.00));   // violates @Positive
        badPrice.setStock(5);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPrice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").value("Price must be a positive value"));

        verify(productService, never()).create(any());
    }

    // ── 5. Validation: negative stock → 400 ──────────────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void postProduct_returns400_withStockError_whenStockIsNegative() throws Exception {
        ProductDTO badStock = new ProductDTO();
        badStock.setSellerId(1L);
        badStock.setName("Tent");
        badStock.setPrice(BigDecimal.valueOf(99.99));
        badStock.setStock(-1);   // violates @PositiveOrZero

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStock)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stock").value("Stock must be zero or a positive value"));

        verify(productService, never()).create(any());
    }
}
