package com.wildbeyond.service;

import com.wildbeyond.model.BlogPost;
import com.wildbeyond.model.Product;
import com.wildbeyond.repository.BlogPostRepository;
import com.wildbeyond.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomepageServiceTest {

    @Mock
    private BlogPostRepository blogRepo;

    @Mock
    private ProductRepository productRepo;

    @InjectMocks
    private HomepageService homepageService;

    @Test
    void getFeaturedBlogs_shouldReturnTopPublishedBlogs() {
        BlogPost blog = BlogPost.builder()
                .id(1L)
                .title("Arctic Front")
                .published(true)
                .build();

        when(blogRepo.findTop5ByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(blog));

        List<BlogPost> result = homepageService.getFeaturedBlogs();

        assertThat(result).containsExactly(blog);
        verify(blogRepo).findTop5ByPublishedTrueOrderByCreatedAtDesc();
    }

    @Test
    void getFeaturedProducts_shouldReturnTopProducts() {
        Product product = Product.builder()
                .id(7L)
                .name("Explorer Pack")
                .price(BigDecimal.valueOf(89.90))
                .stock(5)
                .build();

        when(productRepo.findTop5ByOrderByIdDesc()).thenReturn(List.of(product));

        List<Product> result = homepageService.getFeaturedProducts();

        assertThat(result).containsExactly(product);
        verify(productRepo).findTop5ByOrderByIdDesc();
    }
}
