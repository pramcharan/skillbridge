package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class ReviewResponse {
    private Long    id;
    private Long    projectId;
    private String  projectTitle;

    private Long    reviewerId;
    private String  reviewerName;
    private String  reviewerAvatar;

    private Long    revieweeId;
    private String  revieweeName;

    private Integer rating;
    private String  comment;
    private Instant createdAt;
}