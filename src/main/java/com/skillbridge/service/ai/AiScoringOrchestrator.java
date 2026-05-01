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

    // simple cache to store enrichment results: "userId-jobId" -> AiMatchResult
    private final java.util.Map<String, AiMatchResult> enrichmentCache = new java.util.concurrent.ConcurrentHashMap<>();

    // ── SYNC: Fast score only (for job listing pages) ─────────────
    public AiMatchResult scoreSync(User freelancer, Job job) {
        String cacheKey = freelancer.getId() + "-" + job.getId();
        AiMatchResult cached = enrichmentCache.get(cacheKey);
        if (cached != null) return cached;

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

        String cacheKey = freelancer.getId() + "-" + job.getId();
        AiMatchResult cached = enrichmentCache.get(cacheKey);
        if (cached != null && cached.isAiEnriched()) {
            if (onComplete != null) onComplete.accept(cached);
            return CompletableFuture.completedFuture(cached);
        }

        // Step 1 — instant weighted score
        AiMatchRequest request = buildRequest(freelancer, job);
        AiMatchResult baseResult = weightedScoringService.calculate(
                request,
                freelancer.getProfileCompletionPct() != null
                        ? freelancer.getProfileCompletionPct() : 0);

        // Step 2 — enrich with AI explanation
        try {
            AiExplanationService aiService = aiExplanationFactory.getProvider();
            log.info("AI Provider Selected: {}", aiService.getProviderName());
            log.info("Enriching match explanation with {}", aiService.getProviderName());

            // Update request with base result for context
            request.setPreCalculatedScore(baseResult.getFinalScore());
            request.setPreCalculatedBadge(baseResult.getBadge());
            request.setMatchedSkills(getMatchedSkills(freelancer, job));
            request.setMissingSkills(getMissingSkills(freelancer, job));

            log.info("Calling AI for enrichment...");

            String enrichedExplanation = aiService.enrichExplanation(request);

            log.info("AI Response: {}", enrichedExplanation);
            if (enrichedExplanation != null && !enrichedExplanation.isBlank()) {
                AiMatchResult enrichedResult = AiMatchResult.builder()
                        .finalScore(baseResult.getFinalScore())
                        .badge(baseResult.getBadge())
                        .explanation(enrichedExplanation)
                        .encouragement(baseResult.getEncouragement())
                        .aiEnriched(true)
                        .provider(aiService.getProviderName())
                        .matchedSkills(baseResult.getMatchedSkills())
                        .missingSkills(baseResult.getMissingSkills())
                        .build();

                enrichmentCache.put(cacheKey, enrichedResult);
                if (onComplete != null) onComplete.accept(enrichedResult);
                return CompletableFuture.completedFuture(enrichedResult);
            }
            else {
                log.warn("AI returned NULL or EMPTY → Falling back to algorithm");
            }
        } catch (Exception e) {
            log.error("AI enrichment failed: {}", e.getMessage(), e);
        }

        // Fallback to base result if AI fails — still cache it as "finished"
        // but keep aiEnriched=false if we want to distinguish, 
        // however for polling we want to stop so we should set it or the frontend needs to handle it.
        // Let's set a flag or just assume after 3-5 tries we give up.
        // Alternative: set aiEnriched to true as in "checked finished"
        baseResult.setAiEnriched(true);
        enrichmentCache.put(cacheKey, baseResult);

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