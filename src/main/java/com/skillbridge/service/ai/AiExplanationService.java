package com.skillbridge.service.ai;

import com.skillbridge.dto.ai.AiMatchRequest;

public interface AiExplanationService {

    /**
     * Takes the pre-calculated score and enriches the explanation
     * using natural language understanding of bio + job description.
     *
     * @return enriched explanation string, or null if AI fails
     */
    String enrichExplanation(AiMatchRequest request);

    /**
     * Provider name for logging and response metadata.
     */
    String getProviderName();
}