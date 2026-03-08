package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class AdminJobResponse {
    private Long    id;
    private String  title;
    private String  category;
    private String  status;
    private Double  budget;
    private String  clientName;
    private String  clientEmail;
    private int     proposalCount;
    private Instant createdAt;
    private Instant deadline;
}