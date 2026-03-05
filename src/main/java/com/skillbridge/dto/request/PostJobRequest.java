package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.JobCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class PostJobRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be 5–150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 30, message = "Description must be at least 30 characters")
    private String description;

    @NotNull(message = "Category is required")
    private JobCategory category;

    @NotBlank(message = "Required skills cannot be empty")
    private String requiredSkills; // comma-separated

    @Positive(message = "Budget must be a positive number")
    private Double budget;

    private Instant deadline;
    private Instant autoExpireAt;
}