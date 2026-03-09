package com.skillbridge.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitProposalRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotBlank(message = "Cover letter is required")
    @Size(min = 50, max = 10000,
            message = "Cover letter must be 50–5000 characters")
    private String coverLetter;

    private String attachmentUrl;

    @NotNull(message = "Expected rate is required")
    @Positive(message = "Expected rate must be positive")
    private Double expectedRate;
}