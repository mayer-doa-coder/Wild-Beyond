package com.wildbeyond.controller;

import tools.jackson.databind.ObjectMapper;
import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.controller.rest.ProductRestController;
import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.exception.ResourceNotFoundException;
import com.wildbeyond.model.Product;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for ProductRestController using MockMvc.
 *
 * @WebMvcTest loads only the web layer (controllers, filters, security).
 * Services are replaced with Mockito mocks via @MockitoBean.
 * CustomUserDetailsService is mocked because SecurityConfig depends on it.
 *
 * Role simulation is done with @WithMockUser(roles = "...").
 * Write operations include .with(csrf()) to satisfy CSRF protection.
 */
@WebMvcTest(ProductRestController.class)
@Import(SecurityConfig.class)
class ProductRestControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // Required by SecurityConfig — not used in assertions
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Product product;
    private ProductDTO dto;

    @BeforeEach
    void setUp() {
        // Build MockMvc with springSecurity() so @WithMockUser propagates
        // through the Spring Security filter chain during test execution.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        product = Product.builder()
                .id(10L)
                .name("Tent")
                .description("A sturdy tent")
                .price(BigDecimal.valueOf(199.99))
                .stock(50)
                .build();

        dto = new ProductDTO();
        dto.setSellerId(1L);
        dto.setName("Tent");
        dto.setDescription("A sturdy tent");
        dto.setPrice(BigDecimal.valueOf(199.99));
        dto.setStock(50);
    }

    // ── GET /api/products (public) ────────────────────────────────────────────

    @Test
    void getAll_returns200_withProductList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Tent"));
    }

    @Test
    void getAll_returns200_withEmptyList_whenNoProducts() throws Exception {
        when(productService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/products/{id} (public) ──────────────────────────────────────

    @Test
    void getById_returns200_whenProductExists() throws Exception {
        when(productService.findById(10L)).thenReturn(product);

        mockMvc.perform(get("/api/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Tent"))
                .andExpect(jsonPath("$.price").value(199.99));
    }

    @Test
    void getById_returns404_whenProductMissing() throws Exception {
        when(productService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found with id: 99"));
    }

    // ── POST /api/products (SELLER / ADMIN only) ──────────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void create_returns201_whenSellerAuthorized() throws Exception {
        when(productService.create(any(ProductDTO.class))).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Tent"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void create_returns201_whenAdminAuthorized() throws Exception {
        when(productService.create(any(ProductDTO.class))).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void create_returns400_whenBodyIsInvalid() throws Exception {
        // Send a completely empty DTO — all @NotNull / @NotBlank fields are absent
        // GlobalExceptionHandler catches MethodArgumentNotValidException → 400
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Product name is required"))
                .andExpect(jsonPath("$.price").value("Price is required"))
                .andExpect(jsonPath("$.stock").value("Stock quantity is required"))
                .andExpect(jsonPath("$.sellerId").value("Seller ID is required"));

        verify(productService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void create_returns403_whenBuyerTriesToCreate() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(productService, never()).create(any());
    }

    @Test
    void create_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/products/{id} (SELLER / ADMIN only) ──────────────────────────

    @Test
    @WithMockUser(roles = "SELLER")
    void update_returns200_whenSellerAuthorized() throws Exception {
        when(productService.update(eq(10L), any(ProductDTO.class))).thenReturn(product);

        mockMvc.perform(put("/api/products/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void update_returns403_whenBuyerTriesToUpdate() throws Exception {
        mockMvc.perform(put("/api/products/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(productService, never()).update(any(), any());
    }

    // ── DELETE /api/products/{id} (SELLER / ADMIN only) ──────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204_whenAdminAuthorized() throws Exception {
        doNothing().when(productService).delete(10L);

        mockMvc.perform(delete("/api/products/10").with(csrf()))
                .andExpect(status().isNoContent());

        verify(productService).delete(10L);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void delete_returns403_whenBuyerTriesToDelete() throws Exception {
        mockMvc.perform(delete("/api/products/10").with(csrf()))
                .andExpect(status().isForbidden());

        verify(productService, never()).delete(any());
    }
}
