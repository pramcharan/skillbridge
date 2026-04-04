package com.skillbridge.service.ai;

import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.AvailabilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiScoringOrchestratorTest {

    @Mock
    private WeightedScoringService weightedScoringService;

    @Mock
    private AiExplanationFactory aiExplanationFactory;

    @Mock
    private AiExplanationService aiExplanationService;

    private AiScoringOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AiScoringOrchestrator(weightedScoringService, aiExplanationFactory);
    }

    @Test
    void scoreSync_returnsWeightedScore() {
        User freelancer = buildFreelancer();
        Job job = buildJob();

        WeightedScoringService.SkillMatchResult skillMatch =
                new WeightedScoringService.SkillMatchResult(
                        50.0,
                        java.util.List.of("Java"),
                        java.util.List.of("Docker")
                );

        AiMatchResult baseResult = AiMatchResult.builder()
                .finalScore(72.5)
                .badge("AMBER")
                .explanation("Base explanation")
                .encouragement("Base encouragement")
                .aiEnriched(false)
                .provider("algorithm")
                .build();

        when(weightedScoringService.calculateSkillMatch(any(), any())).thenReturn(skillMatch);
        when(weightedScoringService.calculate(any(), anyInt())).thenReturn(baseResult);

        AiMatchResult result = orchestrator.scoreSync(freelancer, job);

        assertSame(baseResult, result);
    }

    @Test
    void scoreAsync_returnsEnrichedResult_whenAiReturnsExplanation() throws Exception {
        User freelancer = buildFreelancer();
        Job job = buildJob();

        WeightedScoringService.SkillMatchResult skillMatch =
                new WeightedScoringService.SkillMatchResult(
                        50.0,
                        java.util.List.of("Java"),
                        java.util.List.of("Docker")
                );

        AiMatchResult baseResult = AiMatchResult.builder()
                .finalScore(78.0)
                .badge("GREEN")
                .explanation("Base explanation")
                .encouragement("Base encouragement")
                .aiEnriched(false)
                .provider("algorithm")
                .build();

        when(weightedScoringService.calculateSkillMatch(any(), any())).thenReturn(skillMatch);
        when(weightedScoringService.calculate(any(), anyInt())).thenReturn(baseResult);
        when(aiExplanationFactory.getProvider()).thenReturn(aiExplanationService);
        when(aiExplanationService.getProviderName()).thenReturn("ollama");
        when(aiExplanationService.enrichExplanation(any()))
                .thenReturn("Strong skill match with relevant experience. Submit confidently.");

        AtomicReference<AiMatchResult> callbackResult = new AtomicReference<>();

        CompletableFuture<AiMatchResult> future =
                orchestrator.scoreAsync(freelancer, job, callbackResult::set);

        AiMatchResult result = future.get();

        assertNotNull(result);
        assertEquals(78.0, result.getFinalScore());
        assertEquals("GREEN", result.getBadge());
        assertEquals("Strong skill match with relevant experience. Submit confidently.",
                result.getExplanation());
        assertEquals("Base encouragement", result.getEncouragement());
        assertTrue(result.isAiEnriched());
        assertEquals("ollama", result.getProvider());

        assertNotNull(callbackResult.get());
        assertEquals(result.getExplanation(), callbackResult.get().getExplanation());
    }

    @Test
    void scoreAsync_returnsBaseResult_whenAiReturnsNull() throws Exception {
        User freelancer = buildFreelancer();
        Job job = buildJob();

        WeightedScoringService.SkillMatchResult skillMatch =
                new WeightedScoringService.SkillMatchResult(
                        50.0,
                        java.util.List.of("Java"),
                        java.util.List.of("Docker")
                );

        AiMatchResult baseResult = AiMatchResult.builder()
                .finalScore(60.0)
                .badge("AMBER")
                .explanation("Base explanation")
                .encouragement("Base encouragement")
                .aiEnriched(false)
                .provider("algorithm")
                .build();

        when(weightedScoringService.calculateSkillMatch(any(), any())).thenReturn(skillMatch);
        when(weightedScoringService.calculate(any(), anyInt())).thenReturn(baseResult);
        when(aiExplanationFactory.getProvider()).thenReturn(aiExplanationService);
        when(aiExplanationService.getProviderName()).thenReturn("ollama");
        when(aiExplanationService.enrichExplanation(any())).thenReturn(null);

        AtomicReference<AiMatchResult> callbackResult = new AtomicReference<>();

        CompletableFuture<AiMatchResult> future =
                orchestrator.scoreAsync(freelancer, job, callbackResult::set);

        AiMatchResult result = future.get();

        assertSame(baseResult, result);
        assertSame(baseResult, callbackResult.get());
    }

    @Test
    void scoreAsync_returnsBaseResult_whenAiThrowsException() throws Exception {
        User freelancer = buildFreelancer();
        Job job = buildJob();

        WeightedScoringService.SkillMatchResult skillMatch =
                new WeightedScoringService.SkillMatchResult(
                        50.0,
                        java.util.List.of("Java"),
                        java.util.List.of("Docker")
                );

        AiMatchResult baseResult = AiMatchResult.builder()
                .finalScore(55.0)
                .badge("AMBER")
                .explanation("Base explanation")
                .encouragement("Base encouragement")
                .aiEnriched(false)
                .provider("algorithm")
                .build();

        when(weightedScoringService.calculateSkillMatch(any(), any())).thenReturn(skillMatch);
        when(weightedScoringService.calculate(any(), anyInt())).thenReturn(baseResult);
        when(aiExplanationFactory.getProvider()).thenReturn(aiExplanationService);
        when(aiExplanationService.getProviderName()).thenReturn("ollama");
        when(aiExplanationService.enrichExplanation(any()))
                .thenThrow(new RuntimeException("AI failed"));

        AtomicReference<AiMatchResult> callbackResult = new AtomicReference<>();

        CompletableFuture<AiMatchResult> future =
                orchestrator.scoreAsync(freelancer, job, callbackResult::set);

        AiMatchResult result = future.get();

        assertSame(baseResult, result);
        assertSame(baseResult, callbackResult.get());
    }

    @Test
    void scoreSync_usesZeroProfileCompletion_whenNull() {
        User freelancer = buildFreelancer();
        freelancer.setProfileCompletionPct(null);

        Job job = buildJob();

        WeightedScoringService.SkillMatchResult skillMatch =
                new WeightedScoringService.SkillMatchResult(
                        50.0,
                        java.util.List.of("Java"),
                        java.util.List.of("Docker")
                );

        AiMatchResult baseResult = AiMatchResult.builder()
                .finalScore(40.0)
                .badge("RED")
                .explanation("Base explanation")
                .encouragement("Base encouragement")
                .aiEnriched(false)
                .provider("algorithm")
                .build();

        when(weightedScoringService.calculateSkillMatch(any(), any())).thenReturn(skillMatch);
        when(weightedScoringService.calculate(any(), anyInt())).thenReturn(baseResult);

        AiMatchResult result = orchestrator.scoreSync(freelancer, job);

        assertSame(baseResult, result);
    }

    private User buildFreelancer() {
        User user = new User();
        user.setName("Ram");
        user.setBio("Java Spring developer");
        user.setSkills("Java, Spring, React");
        user.setHourlyRate(50.0);
        user.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        user.setProfileCompletionPct(85);
        return user;
    }

    private Job buildJob() {
        Job job = new Job();
        job.setTitle("Backend Developer");
        job.setDescription("Need Java Spring and Docker skills for backend work.");
        job.setRequiredSkills("Java, Docker");
        job.setBudget(100.0);
        return job;
    }
}