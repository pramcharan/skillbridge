package com.skillbridge.dto.request;

import com.skillbridge.entity.enums.AvailabilityStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    private String name;

    @Size(max = 500, message = "Bio must be under 500 characters")
    private String bio;

    private String             skills;       // comma-separated
    private Double             hourlyRate;
    private String             avatarUrl;
    private AvailabilityStatus availabilityStatus;
}