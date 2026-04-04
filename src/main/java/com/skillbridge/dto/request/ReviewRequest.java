package com.skillbridge.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private int rating;
    private String comment;
    private Long projectId;

    public ReviewRequest(int rating, String comment, Long projectId) {
        this.rating = rating;
        this.comment = comment;
        this.projectId = projectId;
    }
}
