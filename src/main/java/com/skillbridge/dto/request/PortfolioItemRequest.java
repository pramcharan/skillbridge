package com.skillbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PortfolioItemRequest {
    @NotBlank
    private String title;
    private String description;
    private String projectUrl;
    private String imageUrl;
    private String tags;      // comma-separated
    private String category;
}
