package com.wildbeyond.repository;

import com.wildbeyond.model.BlogPost;
import com.wildbeyond.model.Product;
import com.wildbeyond.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HomepageRepositoryTest {

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void blogRepository_shouldReturnOnlyPublishedAndLimitToTopFive() {
        for (int index = 1; index <= 6; index++) {
            blogPostRepository.save(BlogPost.builder()
                    .title("Published " + index)
                    .content("Body " + index)
                    .published(true)
                    .build());
        }

        blogPostRepository.save(BlogPost.builder()
                .title("Draft")
                .content("Hidden")
                .published(false)
                .build());

        List<BlogPost> results = blogPostRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc();

        assertThat(results).hasSize(5);
        assertThat(results).allMatch(BlogPost::isPublished);
    }

    @Test
    void productRepository_shouldReturnTopFiveByLatestId() {
        User seller = userRepository.save(User.builder()
                .name("Seller One")
                .email("seller@example.com")
                .password("encoded-password")
                .enabled(true)
                .build());

        for (int index = 1; index <= 7; index++) {
            productRepository.save(Product.builder()
                    .name("Product " + index)
                    .description("Desc " + index)
                    .price(BigDecimal.valueOf(index * 10L))
                    .stock(index)
                    .seller(seller)
                    .build());
        }

        List<Product> results = productRepository.findTop5ByOrderByIdDesc();

        assertThat(results).hasSize(5);
        assertThat(results.get(0).getId()).isGreaterThan(results.get(4).getId());
    }
}
