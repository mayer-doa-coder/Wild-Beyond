package com.wildbeyond.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BlogArticleView {
    Long localId;
    String title;
    String summary;
    String imageUrl;
    String sourceName;
    String sourceUrl;
    LocalDateTime publishedAt;

    public boolean isInternal() {
        return localId != null;
    }
}
