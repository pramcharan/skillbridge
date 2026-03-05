package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class JobCardResponse {
    private Long         id;
    private String       title;
    private JobCategory  category;
    private JobStatus    status;
    private Double       budget;
    private Instant      deadline;
    private List<String> requiredSkills;
    private Integer      proposalCount;
    private String       clientName;
    private Instant      createdAt;
    // Populated per-user when freelancer is logged in
    private Double       aiPreviewScore;
    private String       aiPreviewBadge; // GREEN, AMBER, RED
}