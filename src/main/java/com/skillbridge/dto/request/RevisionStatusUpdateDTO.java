package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.RevisionStatus;

public record RevisionStatusUpdateDTO(
        RevisionStatus status,
        String resolutionNote   // optional, used when RESOLVED or REJECTED
) {}
