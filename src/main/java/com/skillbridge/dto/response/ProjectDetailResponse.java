package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.ProjectStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class ProjectDetailResponse {
    private Long          id;
    private String        title;
    private ProjectStatus status;
    private Instant       createdAt;

    // Client info
    private Long   clientId;
    private String clientName;
    private String clientAvatar;

    // Freelancer info
    private Long   freelancerId;
    private String freelancerName;
    private String freelancerAvatar;

    // Job info
    private Long   jobId;
    private String jobTitle;
    private Double budget;

    // Proposal info
    private Double acceptedRate;

    // Unread message count
    private int unreadCount;
}