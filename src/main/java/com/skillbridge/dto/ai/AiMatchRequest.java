package com.skillbridge.dto.ai;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AiMatchRequest {
    // Freelancer info
    private String       freelancerName;
    private String       freelancerBio;
    private List<String> freelancerSkills;
    private Double       freelancerRate;
    private String       freelancerAvailability;

    // Job info
    private String       jobTitle;
    private String       jobDescription;
    private List<String> requiredSkills;
    private Double       jobBudget;

    // Pre-calculated score from weighted algorithm
    private Double  preCalculatedScore;
    private String  preCalculatedBadge;
    private List<String> matchedSkills;
    private List<String> missingSkills;
}