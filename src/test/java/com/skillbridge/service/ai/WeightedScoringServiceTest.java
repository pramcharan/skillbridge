package com.skillbridge.service.ai;

import com.skillbridge.dto.ai.AiMatchRequest;
import com.skillbridge.dto.ai.AiMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightedScoringServiceTest {

    private WeightedScoringService service;

    @BeforeEach
    void setUp() {
        service = new WeightedScoringService();
    }

    @Test
    void calculateSkillMatch_returns100_whenRequiredSkillsEmpty() {
        WeightedScoringService.SkillMatchResult result =
                service.calculateSkillMatch(List.of("Java", "Spring"), List.of());

        assertEquals(100.0, result.score());
        assertTrue(result.matched().isEmpty());
        assertTrue(result.missing().isEmpty());
    }

    @Test
    void calculateSkillMatch_returns0_whenFreelancerSkillsEmpty() {
        WeightedScoringService.SkillMatchResult result =
                service.calculateSkillMatch(List.of(), List.of("Java", "Spring"));

        assertEquals(0.0, result.score());
        assertTrue(result.matched().isEmpty());
        assertEquals(List.of("Java", "Spring"), result.missing());
    }

    @Test
    void calculateSkillMatch_matchesExactSkills() {
        WeightedScoringService.SkillMatchResult result =
                service.calculateSkillMatch(
                        List.of("Java", "Spring"),
                        List.of("Java", "Spring", "Docker")
                );

        assertEquals(66.66666666666666, result.score());
        assertEquals(List.of("Java", "Spring"), result.matched());
        assertEquals(List.of("Docker"), result.missing());
    }

    @Test
    void calculateSkillMatch_matchesAliasesAndFuzzyVariants() {
        WeightedScoringService.SkillMatchResult result =
                service.calculateSkillMatch(
                        List.of("ReactJS", "Node.js", "PostgreSQL", "Spring Boot"),
                        List.of("React", "Node", "Postgres", "Spring Boot", "Docker")
                );

        assertEquals(80.0, result.score());
        assertEquals(List.of("React", "Node", "Postgres", "Spring Boot"), result.matched());
        assertEquals(List.of("Docker"), result.missing());
    }

    @Test
    void scoreToBadge_returnsGreen_whenScoreAtLeast75() {
        assertEquals("GREEN", service.scoreToBadge(75.0));
        assertEquals("GREEN", service.scoreToBadge(90.0));
    }

    @Test
    void scoreToBadge_returnsAmber_whenScoreBetween45And74() {
        assertEquals("AMBER", service.scoreToBadge(45.0));
        assertEquals("AMBER", service.scoreToBadge(74.9));
    }

    @Test
    void scoreToBadge_returnsRed_whenScoreBelow45() {
        assertEquals("RED", service.scoreToBadge(44.9));
        assertEquals("RED", service.scoreToBadge(10.0));
    }

    @Test
    void calculate_returnsExpectedResult_forStrongMatch() {
        AiMatchRequest request = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java", "Spring", "Docker"))
                .requiredSkills(List.of("Java", "Spring"))
                .freelancerRate(40.0)
                .jobBudget(100.0)
                .freelancerAvailability("AVAILABLE")
                .build();

        AiMatchResult result = service.calculate(request, 90);

        assertNotNull(result);
        assertEquals("GREEN", result.getBadge());
        assertFalse(result.isAiEnriched());
        assertEquals("algorithm", result.getProvider());
        assertEquals(List.of("Java", "Spring"), result.getMatchedSkills());
        assertEquals(List.of(), result.getMissingSkills());
        assertNotNull(result.getExplanation());
        assertNotNull(result.getEncouragement());
        assertTrue(result.getFinalScore() > 0);
    }

    @Test
    void calculate_returnsRedAndMissingSkillEncouragement_forWeakMatch() {
        AiMatchRequest request = AiMatchRequest.builder()
                .freelancerSkills(List.of("HTML"))
                .requiredSkills(List.of("Java", "Spring"))
                .freelancerRate(200.0)
                .jobBudget(100.0)
                .freelancerAvailability("NOT_AVAILABLE")
                .build();

        AiMatchResult result = service.calculate(request, 40);

        assertNotNull(result);
        assertEquals("RED", result.getBadge());
        assertEquals(List.of(), result.getMatchedSkills());
        assertEquals(List.of("Java", "Spring"), result.getMissingSkills());
        assertTrue(result.getExplanation().contains("Missing: Java, Spring"));
        assertTrue(result.getExplanation().contains("hourly rate may exceed"));
        assertTrue(result.getExplanation().contains("Not Available"));
        assertTrue(result.getEncouragement().contains("Consider adding Java"));
    }

    @Test
    void calculate_usesProfileCompletionEncouragement_whenNoMissingSkillsAndLowProfile() {
        AiMatchRequest request = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java", "Spring"))
                .requiredSkills(List.of("Java", "Spring"))
                .freelancerRate(50.0)
                .jobBudget(100.0)
                .freelancerAvailability("OPEN_TO_OFFERS")
                .build();

        AiMatchResult result = service.calculate(request, 60);

        assertTrue(result.getMissingSkills().isEmpty());
        assertTrue(result.getEncouragement().contains("Complete your profile"));
    }

    @Test
    void calculate_usesGreatMatchEncouragement_whenNoMissingSkillsAndHighProfile() {
        AiMatchRequest request = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java", "Spring"))
                .requiredSkills(List.of("Java", "Spring"))
                .freelancerRate(50.0)
                .jobBudget(100.0)
                .freelancerAvailability("AVAILABLE")
                .build();

        AiMatchResult result = service.calculate(request, 95);

        assertTrue(result.getMissingSkills().isEmpty());
        assertEquals("Great match! Submit a strong cover letter to stand out.",
                result.getEncouragement());
    }

    @Test
    void calculate_handlesNullBudgetAndRate_neutrally() {
        AiMatchRequest request = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java"))
                .requiredSkills(List.of("Java"))
                .freelancerRate(null)
                .jobBudget(null)
                .freelancerAvailability(null)
                .build();

        AiMatchResult result = service.calculate(request, 50);

        assertNotNull(result);
        assertEquals(List.of("Java"), result.getMatchedSkills());
        assertEquals(List.of(), result.getMissingSkills());
        assertTrue(result.getFinalScore() > 0);
    }
}