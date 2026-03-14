package com.wildbeyond.controller;

import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.UserRepository;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getProducts_shouldRenderProductsPage() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                Product.builder().id(1L).name("Tent").price(BigDecimal.TEN).stock(5).build()
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("newProduct"));
    }

    @Test
    void createProduct_shouldCreateWithAuthenticatedSeller() throws Exception {
        when(userRepository.findByEmail("seller@example.com")).thenReturn(Optional.of(
                User.builder().id(22L).name("Seller").email("seller@example.com").password("x").build()
        ));
        when(productService.create(any(ProductDTO.class))).thenReturn(
                Product.builder().id(11L).name("Field Kit").price(BigDecimal.valueOf(29.99)).stock(2).build()
        );

        mockMvc.perform(post("/products")
                        .with(user("seller@example.com").roles("SELLER"))
                        .with(csrf())
                        .param("sellerId", "0")
                        .param("name", "Field Kit")
                        .param("description", "Portable kit")
                        .param("price", "29.99")
                        .param("stock", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        ArgumentCaptor<ProductDTO> captor = ArgumentCaptor.forClass(ProductDTO.class);
        verify(productService).create(captor.capture());
        ProductDTO sent = captor.getValue();
        assertThat(sent.getSellerId()).isEqualTo(22L);
        assertThat(sent.getName()).isEqualTo("Field Kit");
    }

    @Test
    void createProduct_shouldRejectInvalidInput() throws Exception {
        mockMvc.perform(post("/products")
                                                .with(user("seller@example.com").roles("SELLER"))
                        .with(csrf())
                        .param("name", "")
                        .param("price", "")
                        .param("stock", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService, never()).create(any());
    }
}
