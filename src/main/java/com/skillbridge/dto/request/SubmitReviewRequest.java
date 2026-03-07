package com.skillbridge.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitReviewRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment too long")
    private String comment;
}