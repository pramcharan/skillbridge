package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.JobStatus;
import lombok.Data;

@Data
public class UpdateJobRequest {
    private String    title;
    private String    description;
    private String    requiredSkills;
    private Double    budget;
    private JobStatus status;
}