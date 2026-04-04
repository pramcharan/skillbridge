package com.skillbridge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.User;
import com.skillbridge.entity.Proposal;
import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiScoringService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public AiMatchResult scoreSync(User freelancer, Job job, Proposal proposal) {
        try {
            String prompt = buildPrompt(freelancer, job);
            String ollamaResponse = callOllama(prompt);
            AiMatchResult result = parseAiResponse(ollamaResponse);
            proposal.setAiMatchScore(result.getFinalScore());
            proposalRepository.save(proposal);
            return result;
        } catch (Exception e) {
            AiMatchResult result = fallbackScoring(freelancer, job);
            proposal.setAiMatchScore(result.getFinalScore());
            proposalRepository.save(proposal);
            return result;
        }
    }

    private String buildPrompt(User freelancer, Job job) {
        return String.format("""
            Analyze this job-freelancer match and return a JSON object with the following fields:
            - finalScore: a number from 0 to 100
            - badge: "STRONG_MATCH", "GOOD_MATCH", "WEAK_MATCH", or "NO_MATCH"
            - explanation: short explanation
            - encouragement: tip to improve
            - matchedSkills: array of skills the freelancer has that match the job
            - missingSkills: array of skills the freelancer lacks

            Freelancer skills: %s
            Job required skills: %s
            """, freelancer.getSkills(), job.getRequiredSkills());
    }

    private String callOllama(String prompt) {
        String url = "http://localhost:11434/api/generate";
        String body = String.format("{\"model\": \"llama2\", \"prompt\": \"%s\", \"stream\": false}", prompt.replace("\"", "\\\""));
        return restTemplate.postForObject(url, body, String.class);
    }

    private AiMatchResult parseAiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String innerJson = root.get("response").asText();
        JsonNode json = objectMapper.readTree(innerJson);
        return AiMatchResult.builder()
                .finalScore(json.get("finalScore").asDouble() / 100)
                .badge(json.get("badge").asText())
                .explanation(json.get("explanation").asText())
                .encouragement(json.get("encouragement").asText())
                .matchedSkills(Arrays.asList(objectMapper.convertValue(json.get("matchedSkills"), String[].class)))
                .missingSkills(Arrays.asList(objectMapper.convertValue(json.get("missingSkills"), String[].class)))
                .aiEnriched(true)
                .provider("ollama")
                .build();
    }

    private AiMatchResult fallbackScoring(User freelancer, Job job) {
        List<String> freelancerSkills = Arrays.asList(freelancer.getSkills().split(","));
        List<String> requiredSkills = Arrays.asList(job.getRequiredSkills().split(","));

        Set<String> freelancerSet = new HashSet<>(freelancerSkills);
        Set<String> requiredSet = new HashSet<>(requiredSkills);

        List<String> matched = requiredSet.stream().filter(freelancerSet::contains).collect(Collectors.toList());
        List<String> missing = requiredSet.stream().filter(s -> !freelancerSet.contains(s)).collect(Collectors.toList());

        double score = requiredSet.isEmpty() ? 0 : (double) matched.size() / requiredSet.size() * 100;

        String badge = score >= 80 ? "STRONG_MATCH" : score >= 60 ? "GOOD_MATCH" : score >= 20 ? "WEAK_MATCH" : "NO_MATCH";

        return AiMatchResult.builder()
                .finalScore(score / 100)
                .badge(badge)
                .explanation("Keyword-based match.")
                .encouragement("Add more relevant skills.")
                .matchedSkills(matched)
                .missingSkills(missing)
                .aiEnriched(false)
                .provider("KEYWORD_FALLBACK")
                .build();
    }
}
