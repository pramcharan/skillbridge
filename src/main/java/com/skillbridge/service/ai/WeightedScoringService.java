package com.skillbridge.service.ai;

import com.skillbridge.dto.ai.AiMatchRequest;
import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.entity.enums.AvailabilityStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeightedScoringService {

    // ── Weights ────────────────────────────────────────────────────
    private static final double WEIGHT_SKILLS       = 0.50;
    private static final double WEIGHT_RATE         = 0.20;
    private static final double WEIGHT_AVAILABILITY = 0.15;
    private static final double WEIGHT_PROFILE      = 0.15;

    // ── Badge thresholds ───────────────────────────────────────────
    private static final double GREEN_THRESHOLD = 75.0;
    private static final double AMBER_THRESHOLD = 45.0;

    public AiMatchResult calculate(AiMatchRequest req,
                                   int profileCompletionPct) {
        // 1. Skill match
        SkillMatchResult skillMatch = calculateSkillMatch(
                req.getFreelancerSkills(),
                req.getRequiredSkills());

        // 2. Rate compatibility
        double rateScore = calculateRateScore(
                req.getFreelancerRate(),
                req.getJobBudget());

        // 3. Availability
        double availabilityScore = calculateAvailabilityScore(
                req.getFreelancerAvailability());

        // 4. Profile completeness
        double profileScore = profileCompletionPct;

        // 5. Weighted final score
        double finalScore =
                (skillMatch.score    * WEIGHT_SKILLS)       +
                        (rateScore           * WEIGHT_RATE)         +
                        (availabilityScore   * WEIGHT_AVAILABILITY) +
                        (profileScore        * WEIGHT_PROFILE);

        finalScore = Math.min(100, Math.max(0, finalScore));

        // 6. Badge
        String badge = scoreToBadge(finalScore);

        // 7. Basic explanation (will be enriched by AI later)
        String explanation = buildBasicExplanation(
                skillMatch, rateScore, finalScore,
                req.getFreelancerAvailability());

        return AiMatchResult.builder()
                .finalScore(Math.round(finalScore * 10.0) / 10.0)
                .badge(badge)
                .explanation(explanation)
                .encouragement(buildEncouragement(skillMatch, profileCompletionPct))
                .aiEnriched(false)
                .provider("algorithm")
                .matchedSkills(skillMatch.matched())  // ← ADDED
                .missingSkills(skillMatch.missing())  // ← ADDED
                .build();
    }

    // ── Skill matching with fuzzy normalization ────────────────────
    public SkillMatchResult calculateSkillMatch(List<String> freelancerSkills,
                                                List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return new SkillMatchResult(100.0, List.of(), List.of());
        }
        if (freelancerSkills == null || freelancerSkills.isEmpty()) {
            return new SkillMatchResult(0.0, List.of(), requiredSkills);
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        List<String> normalizedFreelancer = requiredSkills.stream()
                .map(this::normalize)
                .toList();

        for (String required : requiredSkills) {
            String normalizedRequired = normalize(required);
            boolean found = freelancerSkills.stream()
                    .anyMatch(fs -> fuzzyMatch(normalize(fs), normalizedRequired));
            if (found) matched.add(required);
            else        missing.add(required);
        }

        double score = (double) matched.size() / requiredSkills.size() * 100.0;
        return new SkillMatchResult(score, matched, missing);
    }

    // ── Fuzzy matching — handles React/ReactJS, Node/Node.js ──────
    private boolean fuzzyMatch(String a, String b) {
        if (a.equals(b))             return true;
        if (a.contains(b) || b.contains(a)) return true;

        // Strip common suffixes: js, .js, framework
        String aClean = a.replaceAll("(\\.js|js|framework|lang|script)$", "");
        String bClean = b.replaceAll("(\\.js|js|framework|lang|script)$", "");
        if (aClean.equals(bClean))   return true;

        // Common aliases
        if (isAlias(a, b))           return true;

        return false;
    }

    private boolean isAlias(String a, String b) {
        return pairMatches(a, b, "react",      "reactjs")     ||
                pairMatches(a, b, "node",       "nodejs")      ||
                pairMatches(a, b, "vue",        "vuejs")       ||
                pairMatches(a, b, "postgres",   "postgresql")  ||
                pairMatches(a, b, "mongo",      "mongodb")     ||
                pairMatches(a, b, "k8s",        "kubernetes")  ||
                pairMatches(a, b, "tf",         "tensorflow")  ||
                pairMatches(a, b, "js",         "javascript")  ||
                pairMatches(a, b, "ts",         "typescript")  ||
                pairMatches(a, b, "py",         "python")      ||
                pairMatches(a, b, "springboot", "spring boot");
    }

    private boolean pairMatches(String a, String b, String x, String y) {
        return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
    }

    private String normalize(String skill) {
        return skill.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    // ── Rate scoring ───────────────────────────────────────────────
    private double calculateRateScore(Double freelancerRate, Double jobBudget) {
        if (freelancerRate == null || jobBudget == null || jobBudget == 0) {
            return 70.0; // Neutral if no rate data
        }

        // Assume budget is total project budget, estimate hourly from it
        // If freelancer rate is well under budget → full score
        double ratio = freelancerRate / jobBudget * 100;

        if (ratio <= 60)  return 100.0; // Very affordable
        if (ratio <= 80)  return 85.0;  // Affordable
        if (ratio <= 100) return 70.0;  // Fits budget
        if (ratio <= 120) return 40.0;  // Slightly over
        if (ratio <= 150) return 15.0;  // Over budget
        return 0.0;                      // Way over budget
    }

    // ── Availability scoring ───────────────────────────────────────
    private double calculateAvailabilityScore(String availability) {
        if (availability == null) return 50.0;
        return switch (availability.toUpperCase()) {
            case "AVAILABLE"      -> 100.0;
            case "OPEN_TO_OFFERS" -> 60.0;
            case "NOT_AVAILABLE"  -> 0.0;
            default               -> 50.0;
        };
    }

    // ── Badge ──────────────────────────────────────────────────────
    public String scoreToBadge(double score) {
        if (score >= GREEN_THRESHOLD) return "GREEN";
        if (score >= AMBER_THRESHOLD) return "AMBER";
        return "RED";
    }

    // ── Basic explanation ──────────────────────────────────────────
    private String buildBasicExplanation(SkillMatchResult skillMatch,
                                         double rateScore,
                                         double finalScore,
                                         String availability) {
        int matched = skillMatch.matched().size();
        int total   = matched + skillMatch.missing().size();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("You match %d of %d required skills. ", matched, total));

        if (!skillMatch.missing().isEmpty()) {
            sb.append("Missing: ")
                    .append(String.join(", ", skillMatch.missing()))
                    .append(". ");
        }

        if (rateScore < 40) {
            sb.append("Your hourly rate may exceed the client's budget. ");
        }

        if ("NOT_AVAILABLE".equalsIgnoreCase(availability)) {
            sb.append("Your availability status is set to Not Available. ");
        }

        return sb.toString().trim();
    }

    // ── Encouragement tip ─────────────────────────────────────────
    private String buildEncouragement(SkillMatchResult skillMatch,
                                      int profilePct) {
        if (!skillMatch.missing().isEmpty()) {
            return "Consider adding " + skillMatch.missing().get(0) +
                    " to your skills to boost your match score.";
        }
        if (profilePct < 80) {
            return "Complete your profile to increase your match score by up to 15 points.";
        }
        return "Great match! Submit a strong cover letter to stand out.";
    }

    // ── Inner result record ────────────────────────────────────────
    public record SkillMatchResult(
            double score,
            List<String> matched,
            List<String> missing) {}
}