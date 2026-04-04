package com.skillbridge.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skillbridge.entity.enums.JobStatus;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateJobRequest {
    private String    title;
    private String    description;
    private String category;
    private String    requiredSkills;
    private Double    budget;
    private JobStatus status;
    private String deadline;
}