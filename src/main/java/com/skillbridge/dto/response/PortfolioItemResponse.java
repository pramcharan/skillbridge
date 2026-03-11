package com.skillbridge.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class PortfolioItemResponse {
    private Long         id;
    private String       title;
    private String       description;
    private String       projectUrl;
    private String       imageUrl;
    private List<String> tags;
    private String       category;
    private Instant createdAt;
    private String       ownerName;
    private Long         ownerId;
}