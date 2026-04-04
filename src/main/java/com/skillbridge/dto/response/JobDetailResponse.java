package com.skillbridge.dto.response;

import com.skillbridge.dto.request.JobAttachmentDTO;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.JobStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class JobDetailResponse {
    private Long         id;
    private String       title;
    private String       description;
    private JobCategory  category;
    private JobStatus    status;
    private Double       budget;
    private Instant      deadline;
    private Instant      autoExpireAt;
    private List<String> requiredSkills;
    private List<JobAttachmentDTO> attachments;
    private Integer      proposalCount;
    private Long         clientId;
    private String       clientName;
    private Double       clientRating;
    private Instant      createdAt;
    // AI fields — populated for logged-in freelancer
    private Double       aiPreviewScore;
    private String       aiPreviewBadge;
    private String       aiPreviewReason;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Boolean      alreadyApplied;
}