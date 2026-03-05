package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.AvailabilityStatus;
import com.skillbridge.entity.enums.Role;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserProfileResponse {
    private Long               id;
    private String             name;
    private String             email;
    private Role               role;
    private String             bio;
    private String             avatarUrl;
    private List<String>       skills;
    private Double             hourlyRate;
    private Double             avgRating;
    private Integer            reviewCount;
    private Integer            profileCompletionPct;
    private AvailabilityStatus availabilityStatus;
    private Boolean            isEmailVerified;
    private Instant            createdAt;
    private Instant            lastActive;
    // Sensitive fields (email, passwordHash) are NEVER included
}