package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProposalStatusRequest {

    @NotNull(message = "Status is required")
    private ProposalStatus status; // ACCEPTED, REJECTED
}