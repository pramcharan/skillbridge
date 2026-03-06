package com.skillbridge.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.config.AiConfig;
import com.skillbridge.dto.ai.AiMatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("openai")
@RequiredArgsConstructor
public class OpenAiExplanationService implements AiExplanationService {

    private final AiConfig     aiConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String enrichExplanation(AiMatchRequest request) {
        try {
            String prompt = buildPrompt(request);
            return callOpenAi(prompt);
        } catch (Exception e) {
            log.warn("OpenAI enrichment failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    private String buildPrompt(AiMatchRequest req) {
        // Same prompt structure as Ollama
        return String.format("""
            Analyze this freelancer-job match and respond ONLY in JSON:
            {
              "explanation": "2-3 sentence professional match explanation",
              "encouragement": "1 sentence improvement tip"
            }
            
            Freelancer skills: %s
            Job required skills: %s
            Match score: %.0f/100
            Matched: %s
            Missing: %s
            Job description (first 200 chars): %s
            """,
                String.join(", ", req.getFreelancerSkills()),
                String.join(", ", req.getRequiredSkills()),
                req.getPreCalculatedScore(),
                String.join(", ", req.getMatchedSkills()),
                String.join(", ", req.getMissingSkills()),
                req.getJobDescription() != null ?
                        req.getJobDescription().substring(0,
                                Math.min(200, req.getJobDescription().length())) : ""
        );
    }

    private String callOpenAi(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", aiConfig.getOpenAiModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 200
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getOpenAiBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiConfig.getOpenAiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI returned HTTP " + response.statusCode());
        }

        JsonNode root    = objectMapper.readTree(response.body());
        String content   = root.path("choices").get(0)
                .path("message").path("content").asText();

        // Parse JSON from response
        int start = content.indexOf("{");
        int end   = content.lastIndexOf("}");
        if (start == -1 || end == -1) return null;

        JsonNode node        = objectMapper.readTree(content.substring(start, end + 1));
        String explanation   = node.path("explanation").asText(null);
        String encouragement = node.path("encouragement").asText(null);

        if (explanation == null) return null;
        return encouragement != null ? explanation + " " + encouragement : explanation;
    }
}