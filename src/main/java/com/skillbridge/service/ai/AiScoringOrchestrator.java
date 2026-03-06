package com.skillbridge.service.ai;

import com.skillbridge.dto.ai.AiMatchRequest;
import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScoringOrchestrator {

    private final WeightedScoringService weightedScoringService;
    private final AiExplanationFactory   aiExplanationFactory;

    // ── SYNC: Fast score only (for job listing pages) ─────────────
    public AiMatchResult scoreSync(User freelancer, Job job) {
        AiMatchRequest request = buildRequest(freelancer, job);
        return weightedScoringService.calculate(
                request,
                freelancer.getProfileCompletionPct() != null
                        ? freelancer.getProfileCompletionPct() : 0);
    }

    // ── ASYNC: Full score + AI enrichment (for job detail page) ───
    @Async
    public CompletableFuture<AiMatchResult> scoreAsync(
            User freelancer,
            Job job,
            Consumer<AiMatchResult> onComplete) {

        // Step 1 — instant weighted score
        AiMatchRequest request = buildRequest(freelancer, job);
        AiMatchResult baseResult = weightedScoringService.calculate(
                request,
                freelancer.getProfileCompletionPct() != null
                        ? freelancer.getProfileCompletionPct() : 0);

        // Step 2 — enrich with AI explanation
        try {
            AiExplanationService aiService = aiExplanationFactory.getProvider();
            log.info("Enriching match explanation with {}", aiService.getProviderName());

            // Update request with base result for context
            request.setPreCalculatedScore(baseResult.getFinalScore());
            request.setPreCalculatedBadge(baseResult.getBadge());
            request.setMatchedSkills(getMatchedSkills(freelancer, job));
            request.setMissingSkills(getMissingSkills(freelancer, job));

            String enrichedExplanation = aiService.enrichExplanation(request);

            if (enrichedExplanation != null && !enrichedExplanation.isBlank()) {
                AiMatchResult enrichedResult = AiMatchResult.builder()
                        .finalScore(baseResult.getFinalScore())
                        .badge(baseResult.getBadge())
                        .explanation(enrichedExplanation)
                        .encouragement(baseResult.getEncouragement())
                        .aiEnriched(true)
                        .provider(aiService.getProviderName())
                        .build();

                if (onComplete != null) onComplete.accept(enrichedResult);
                return CompletableFuture.completedFuture(enrichedResult);
            }
        } catch (Exception e) {
            log.error("AI enrichment failed: {}", e.getMessage());
        }

        // Fallback to base result if AI fails
        if (onComplete != null) onComplete.accept(baseResult);
        return CompletableFuture.completedFuture(baseResult);
    }

    // ── Build request object ───────────────────────────────────────
    private AiMatchRequest buildRequest(User freelancer, Job job) {
        List<String> freelancerSkills = splitSkills(freelancer.getSkills());
        List<String> requiredSkills   = splitSkills(job.getRequiredSkills());

        WeightedScoringService.SkillMatchResult skillMatch =
                weightedScoringService.calculateSkillMatch(
                        freelancerSkills, requiredSkills);

        return AiMatchRequest.builder()
                .freelancerName(freelancer.getName())
                .freelancerBio(freelancer.getBio())
                .freelancerSkills(freelancerSkills)
                .freelancerRate(freelancer.getHourlyRate())
                .freelancerAvailability(
                        freelancer.getAvailabilityStatus() != null
                                ? freelancer.getAvailabilityStatus().name() : "UNKNOWN")
                .jobTitle(job.getTitle())
                .jobDescription(job.getDescription())
                .requiredSkills(requiredSkills)
                .jobBudget(job.getBudget())
                .preCalculatedScore(0.0)
                .preCalculatedBadge("")
                .matchedSkills(skillMatch.matched())
                .missingSkills(skillMatch.missing())
                .build();
    }

    private List<String> getMatchedSkills(User freelancer, Job job) {
        return weightedScoringService.calculateSkillMatch(
                splitSkills(freelancer.getSkills()),
                splitSkills(job.getRequiredSkills())).matched();
    }

    private List<String> getMissingSkills(User freelancer, Job job) {
        return weightedScoringService.calculateSkillMatch(
                splitSkills(freelancer.getSkills()),
                splitSkills(job.getRequiredSkills())).missing();
    }

    private List<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) return List.of();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}