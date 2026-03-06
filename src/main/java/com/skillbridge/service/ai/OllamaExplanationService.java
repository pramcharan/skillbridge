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
import java.util.Map;

@Slf4j
@Service("ollama")
@RequiredArgsConstructor
public class OllamaExplanationService implements AiExplanationService {

    private final AiConfig     aiConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String enrichExplanation(AiMatchRequest request) {
        try {
            String prompt = buildPrompt(request);
            String rawResponse = callOllama(prompt);
            return parseExplanation(rawResponse);
        } catch (Exception e) {
            log.warn("Ollama enrichment failed, falling back to algorithm explanation: {}",
                    e.getMessage());
            return null; // Caller handles null gracefully
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    // ── Build the prompt ───────────────────────────────────────────
    private String buildPrompt(AiMatchRequest req) {
        return String.format("""
            You are an expert freelance marketplace AI. Analyze this job-freelancer match.
            
            FREELANCER:
            - Name: %s
            - Bio: %s
            - Skills: %s
            - Hourly Rate: $%.0f
            - Availability: %s
            
            JOB:
            - Title: %s
            - Description: %s
            - Required Skills: %s
            - Budget: $%.0f
            
            PRE-CALCULATED MATCH:
            - Score: %.0f/100
            - Badge: %s
            - Matched Skills: %s
            - Missing Skills: %s
            
            Write a SHORT, HONEST, PROFESSIONAL match explanation in exactly this JSON format:
            {
              "explanation": "2-3 sentence explanation of why this is a good or poor match based on skills, experience, and bio",
              "encouragement": "1 sentence specific tip to improve this match score"
            }
            
            Rules:
            - Be specific, mention actual skills by name
            - If bio is empty, don't mention it
            - If score is high, be encouraging
            - If score is low, be honest but constructive
            - Return ONLY the JSON, no other text
            """,
                req.getFreelancerName(),
                req.getFreelancerBio() != null ? req.getFreelancerBio() : "Not provided",
                String.join(", ", req.getFreelancerSkills()),
                req.getFreelancerRate() != null ? req.getFreelancerRate() : 0,
                req.getFreelancerAvailability(),
                req.getJobTitle(),
                req.getJobDescription() != null ?
                        req.getJobDescription().substring(0,
                                Math.min(300, req.getJobDescription().length())) : "",
                String.join(", ", req.getRequiredSkills()),
                req.getJobBudget() != null ? req.getJobBudget() : 0,
                req.getPreCalculatedScore(),
                req.getPreCalculatedBadge(),
                String.join(", ", req.getMatchedSkills()),
                String.join(", ", req.getMissingSkills())
        );
    }

    // ── Call Ollama REST API ───────────────────────────────────────
    private String callOllama(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model",  aiConfig.getOllamaModel(),
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.3,  // Low = more consistent output
                        "num_predict", 200   // Max tokens in response
                )
        );

        String requestBody = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getOllamaBaseUrl() + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(aiConfig.getOllamaTimeoutSeconds()))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode());
        }

        // Extract the "response" field from Ollama's JSON
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("response").asText();
    }

    // ── Parse JSON from model response ─────────────────────────────
    private String parseExplanation(String rawResponse) {
        try {
            // Find the JSON block in the response
            int start = rawResponse.indexOf("{");
            int end   = rawResponse.lastIndexOf("}");

            if (start == -1 || end == -1) {
                log.warn("Ollama response did not contain JSON: {}", rawResponse);
                return null;
            }

            String jsonPart = rawResponse.substring(start, end + 1);
            JsonNode node   = objectMapper.readTree(jsonPart);

            String explanation   = node.path("explanation").asText(null);
            String encouragement = node.path("encouragement").asText(null);

            if (explanation == null) return null;

            return encouragement != null
                    ? explanation + " " + encouragement
                    : explanation;

        } catch (Exception e) {
            log.warn("Failed to parse Ollama JSON response: {}", e.getMessage());
            return null;
        }
    }
}