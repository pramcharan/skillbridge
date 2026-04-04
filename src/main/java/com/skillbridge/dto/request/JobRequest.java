package com.skillbridge.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skillbridge.entity.enums.JobCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobRequest {
    private String title;
    private String description;
    private JobCategory category;
    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private List<String> skills;
    private Instant deadline;
    private Instant autoExpireAt;

    public JobRequest(String title, String description, JobCategory category, BigDecimal minBudget, BigDecimal maxBudget, List<String> skills, Instant deadline, Instant autoExpireAt) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.skills = skills;
        this.deadline = deadline;
        this.autoExpireAt = autoExpireAt;
    }
}
