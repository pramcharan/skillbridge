package com.skillbridge.service;

import com.skillbridge.dto.request.OnboardingRequest;
import com.skillbridge.entity.User;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;

    // ── Status check (called after login) ────────────────────────────
    public boolean isOnboardingComplete(String email) {
        return userRepository.findByEmail(email)
                .map(User::isOnboardingComplete)
                .orElse(true);
    }

    @Transactional
    public void completeOnboarding(String email, OnboardingRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ── Name ──────────────────────────────────────────
        if (req.firstName() != null && !req.firstName().isBlank()) {
            String full = req.firstName().trim()
                    + (req.lastName() != null && !req.lastName().isBlank()
                    ? " " + req.lastName().trim() : "");
            user.setName(full);
        }

        // ── Bio / headline combined ────────────────────────
        if (req.bio() != null && !req.bio().isBlank()) {
            user.setBio(req.bio().trim());
        }

        // ── Location ──────────────────────────────────────
        if (req.location() != null && !req.location().isBlank()) {
            user.setLocation(req.location().trim());
        }

        // ── Language ──────────────────────────────────────
        if (req.language() != null && !req.language().isBlank()) {
            user.setLanguage(req.language().trim());
        }

        // ── Skills (only for freelancers) ─────────────────
        if (req.skills() != null && !req.skills().isEmpty()) {
            String skillsCsv = String.join(",", req.skills()
                    .stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList());
            user.setSkills(skillsCsv);
        }

        // ── Freelancer-specific ───────────────────────────
        if (req.hourlyRate() != null && req.hourlyRate() >= 5) {
            user.setHourlyRate(req.hourlyRate());
        }
        if (req.experienceLevel() != null && !req.experienceLevel().isBlank()) {
            user.setExperienceLevel(req.experienceLevel().trim());
        }
        if (req.availability() != null && !req.availability().isBlank()) {
            user.setAvailability(req.availability().trim());
        }
        if (req.portfolioUrl() != null && !req.portfolioUrl().isBlank()) {
            user.setPortfolioUrl(req.portfolioUrl().trim());
        }

        // ── Client-specific ───────────────────────────────
        if (req.companyName() != null && !req.companyName().isBlank()) {
            user.setCompanyName(req.companyName().trim());
        }
        if (req.hiringGoal() != null && !req.hiringGoal().isBlank()) {
            user.setHiringGoal(req.hiringGoal().trim());
        }
        if (req.budgetRange() != null && !req.budgetRange().isBlank()) {
            user.setBudgetRange(req.budgetRange().trim());
        }

        // ── Mark onboarding complete ──────────────────────
        user.setOnboardingComplete(true);

        userRepository.save(user);
    }
}
