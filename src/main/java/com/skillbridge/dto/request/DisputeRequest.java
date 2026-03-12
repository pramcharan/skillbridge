package com.skillbridge.dto.request;
import lombok.Data;

@Data
public class DisputeRequest {
    private Long   projectId;
    private String reason;        // short title
    private String description;   // detailed explanation
    private String evidenceUrls;  // comma-separated URLs (optional)
}