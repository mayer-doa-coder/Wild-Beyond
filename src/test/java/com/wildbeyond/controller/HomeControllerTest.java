package com.wildbeyond.controller;

import com.wildbeyond.config.SecurityConfig;
import com.wildbeyond.model.BlogPost;
import com.wildbeyond.model.Product;
import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.service.HomepageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomepageService homepageService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithAnonymousUser
    void home_shouldRenderHomeWithFallbackStories_whenNoBlogsOrProducts() throws Exception {
        when(homepageService.getFeaturedBlogs()).thenReturn(List.of());
        when(homepageService.getFeaturedProducts()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("blogs"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("stories"))
                .andExpect(model().attributeExists("featuredStory"))
                .andExpect(model().attributeExists("documentaries"))
                .andExpect(model().attributeExists("travelStories"))
                .andExpect(model().attributeExists("issueArticles"));
    }

    @Test
    @WithAnonymousUser
    void blog_shouldRenderHomeWithServiceData() throws Exception {
        BlogPost blog = BlogPost.builder()
                .id(1L)
                .title("River Watch")
                .content("Field report from the delta.")
                .published(true)
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("Trail Camera")
                .description("Weather-sealed")
                .price(BigDecimal.valueOf(149.99))
                .stock(12)
                .build();

        when(homepageService.getFeaturedBlogs()).thenReturn(List.of(blog));
        when(homepageService.getFeaturedProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/blog"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("blogs", List.of(blog)))
                .andExpect(model().attribute("products", List.of(product)))
                .andExpect(model().attributeExists("stories"))
                .andExpect(model().attributeExists("featuredStory"));
    }

    @Test
    @WithAnonymousUser
    void explore_shouldRenderHomeForAnonymousUser() throws Exception {
        when(homepageService.getFeaturedBlogs()).thenReturn(List.of());
        when(homepageService.getFeaturedProducts()).thenReturn(List.of());

        mockMvc.perform(get("/explore"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }
}
