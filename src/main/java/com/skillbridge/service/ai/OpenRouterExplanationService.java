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
@Service("openrouter")
@RequiredArgsConstructor
public class OpenRouterExplanationService implements AiExplanationService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String enrichExplanation(AiMatchRequest request) {
        try {
            String prompt = buildPrompt(request);
            return callOpenRouter(prompt);
        } catch (Exception e) {
            log.warn("OpenRouter failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    private String buildPrompt(AiMatchRequest req) {
        return String.format("""
            Analyze this freelancer-job match and respond ONLY in JSON:
            {
              "explanation": "2-3 sentence explanation",
              "encouragement": "1 sentence tip"
            }

            Skills: %s
            Required: %s
            Score: %.0f
            Matched: %s
            Missing: %s
            """,
                String.join(", ", req.getFreelancerSkills()),
                String.join(", ", req.getRequiredSkills()),
                req.getPreCalculatedScore(),
                String.join(", ", req.getMatchedSkills()),
                String.join(", ", req.getMissingSkills())
        );
    }

    private String callOpenRouter(String prompt) throws Exception {

        // Try PRIMARY model
        try {
            log.info("Trying PRIMARY model: {}", aiConfig.getOpenrouterPrimaryModel());
            return sendRequest(prompt, aiConfig.getOpenrouterPrimaryModel());
        } catch (Exception e) {
            log.warn("Primary model failed: {}", e.getMessage());
        }

        // Try BACKUP model
        try {
            log.info("Trying BACKUP model: {}", aiConfig.getOpenrouterBackupModel());
            return sendRequest(prompt, aiConfig.getOpenrouterBackupModel());
        } catch (Exception e) {
            log.warn("Backup model failed: {}", e.getMessage());
        }

        throw new RuntimeException("All OpenRouter models failed");
    }

    private String sendRequest(String prompt, String model) throws Exception {

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getOpenrouterBaseUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + aiConfig.getOpenrouterApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter error: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        String content = root.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");

        if (start == -1 || end == -1) return null;

        JsonNode json = objectMapper.readTree(content.substring(start, end + 1));

        String explanation = json.path("explanation").asText(null);
        String encouragement = json.path("encouragement").asText(null);

        if (explanation == null) return null;

        return encouragement != null
                ? explanation + " " + encouragement
                : explanation;
    }
}