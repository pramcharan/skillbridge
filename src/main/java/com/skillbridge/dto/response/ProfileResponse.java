package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class ProfileResponse {
    private Long    id;
    private String  name;
    private String  email;
    private String  role;
    private String  bio;
    private String  skills;
    private Double  hourlyRate;
    private String  availability;
    private String  location;
    private String  avatarUrl;
    private String  portfolioUrl;
    private Integer profileCompletionPct;
    private Instant createdAt;

    // Stats
    private Integer totalJobs;
    private Integer totalProposals;
    private Double  avgRating;
    private Integer reviewCount;
}