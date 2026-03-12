package com.skillbridge.dto.request;

import lombok.Data;

@Data
public class DisputeResolveRequest {
    private String resolution;   // FAVOUR_REPORTER | FAVOUR_RESPONDENT | SPLIT | NO_ACTION
    private String adminNotes;
}
