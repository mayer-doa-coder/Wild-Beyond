package com.wildbeyond.service;

import com.wildbeyond.model.BlogPost;
import com.wildbeyond.model.Product;
import com.wildbeyond.repository.BlogPostRepository;
import com.wildbeyond.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HomepageService {

    private final BlogPostRepository blogRepo;
    private final ProductRepository productRepo;

    public List<BlogPost> getFeaturedBlogs() {
        return blogRepo.findTop5ByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Product> getFeaturedProducts() {
        return productRepo.findTop5ByOrderByIdDesc();
    }

    public Optional<BlogPost> findBlogPostById(Long id) {
        return blogRepo.findById(id);
    }
}
