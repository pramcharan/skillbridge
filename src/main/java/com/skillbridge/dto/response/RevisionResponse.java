package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.RevisionStatus;

import java.time.Instant;

public record RevisionResponse(
        Long id,
        Long projectId,
        String projectTitle,
        String requesterName,
        String requesterAvatar,
        boolean requesterIsClient,
        String title,
        String description,
        RevisionStatus status,
        String resolutionNote,
        String resolvedByName,
        Instant createdAt,
        Instant resolvedAt
) {}