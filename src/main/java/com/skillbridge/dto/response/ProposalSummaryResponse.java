package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.ProposalStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ProposalSummaryResponse {
    private Long           id;
    private Long           jobId;
    private String         jobTitle;
    private String         jobCategory;
    private ProposalStatus status;
    private Double         expectedRate;
    private Double         aiMatchScore;
    private String         aiMatchBadge;
    private Instant        createdAt;
    // Shown to freelancer viewing their own proposals
    private String         clientName;
}