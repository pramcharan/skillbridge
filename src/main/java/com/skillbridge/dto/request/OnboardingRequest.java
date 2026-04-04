package com.skillbridge.dto.request;
import java.util.List;

public record OnboardingRequest(
        String firstName,
        String lastName,
        String bio,
        String location,
        String language,
        List<String> skills,
        Double hourlyRate,
        String experienceLevel,
        String availability,
        String portfolioUrl,
        String companyName,
        String hiringGoal,
        String budgetRange,
        boolean onboardingComplete
) {}
