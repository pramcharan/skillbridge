package com.skillbridge.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiMatchResult {
    private Double  finalScore;      // 0–100
    private String  badge;           // GREEN, AMBER, RED
    private String  explanation;     // Human-readable reason
    private String  encouragement;   // Tip to improve score
    private boolean aiEnriched;      // true if Ollama/OpenAI ran
    private String  provider;        // "ollama", "openai", "algorithm"
    // Add to AiMatchResult.java
    private List<String> matchedSkills;
    private List<String> missingSkills;
}