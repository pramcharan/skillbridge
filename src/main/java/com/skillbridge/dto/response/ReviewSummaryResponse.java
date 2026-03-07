package com.skillbridge.dto.response;

import lombok.Data;

@Data
public class ReviewSummaryResponse {
    private Double  averageRating;
    private Long    totalReviews;
    private int     fiveStar;
    private int     fourStar;
    private int     threeStar;
    private int     twoStar;
    private int     oneStar;
}