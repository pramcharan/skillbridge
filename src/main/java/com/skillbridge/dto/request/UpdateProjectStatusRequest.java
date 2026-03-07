package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateProjectStatusRequest {

    @NotNull
    private ProjectStatus status; // COMPLETED, CANCELLED
}