package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class DisputeResponse {
    private Long         id;
    private Long         projectId;
    private String       projectTitle;
    private Long         reporterId;
    private String       reporterName;
    private Long         respondentId;
    private String       respondentName;
    private String       reason;
    private String       description;
    private List<String> evidenceUrls;
    private String       respondentReply;
    private List<String> respondentEvidenceUrls;
    private String       adminNotes;
    private String       status;
    private String       resolution;
    private String       resolvedByName;
    private Instant      createdAt;
    private Instant      resolvedAt;
}
