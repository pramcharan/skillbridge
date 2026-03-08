package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class AdminUserResponse {
    private Long    id;
    private String  name;
    private String  email;
    private String  role;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Integer profileCompletionPct;
    private Double  avgRating;
    private Integer reviewCount;
    private Instant createdAt;
    private Instant lastActive;
    private int     totalJobs;
    private int     totalProposals;
}