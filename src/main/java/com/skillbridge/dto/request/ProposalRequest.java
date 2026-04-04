package com.skillbridge.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProposalRequest {
    private String coverLetter;
    private BigDecimal expectedRate;
    private int durationInDays;

    public ProposalRequest(String coverLetter, BigDecimal expectedRate, int durationInDays) {
        this.coverLetter = coverLetter;
        this.expectedRate = expectedRate;
        this.durationInDays = durationInDays;
    }
}
