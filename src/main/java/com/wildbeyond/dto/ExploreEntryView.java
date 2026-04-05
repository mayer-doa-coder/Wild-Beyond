package com.wildbeyond.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExploreEntryView {
    String category;
    String slug;
    String name;
    String shortDescription;
    String detail;
    String imageUrl;
    String imagePosition;
    String habitat;
    String conservationStatus;
    String sourceUrl;
}
