package com.skillbridge.model;

import lombok.Data;

import java.util.List;

@Data
public class AiMatchResult {
    private double finalScore;
    private List<String> matchedSkills;
    private String provider;
    private boolean aiEnriched;

    public AiMatchResult(double finalScore, List<String> matchedSkills, String provider) {
        this.finalScore = finalScore;
        this.matchedSkills = matchedSkills;
        this.provider = provider;
        this.aiEnriched = !provider.equals("KEYWORD_FALLBACK");
    }
}
