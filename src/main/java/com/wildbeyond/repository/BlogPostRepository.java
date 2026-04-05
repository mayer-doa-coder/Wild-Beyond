package com.wildbeyond.repository;

import com.wildbeyond.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    List<BlogPost> findTop5ByPublishedTrueOrderByCreatedAtDesc();

    List<BlogPost> findByPublishedTrueOrderByCreatedAtDesc();

    java.util.Optional<BlogPost> findByIdAndPublishedTrue(Long id);
}
