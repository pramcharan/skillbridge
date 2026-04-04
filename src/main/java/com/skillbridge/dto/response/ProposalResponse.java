package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.ProposalStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ProposalResponse {
    private Long           id;
    private Long           jobId;
    private String         jobTitle;
    private Long           freelancerId;
    private String         freelancerName;
    private String         freelancerAvatarUrl;
    private Double         freelancerRating;
    private List<String>   freelancerSkills;
    private String         coverLetter;
    private String         attachmentUrl;
    private Double         expectedRate;
    private ProposalStatus status;
    private Boolean        viewedByClient;
    private Instant        createdAt;

    // AI fields
    private Double       aiMatchScore;
    private String       aiMatchBadge;
    private String       aiMatchReason;
    private List<String> matchedSkills;
    private List<String> missingSkills;
}