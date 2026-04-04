package com.wildbeyond.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WildlifePhotoView {
    String title;
    String ownerName;
    String location;
    String detail;
    String imageUrl;
    String imagePosition;
}
