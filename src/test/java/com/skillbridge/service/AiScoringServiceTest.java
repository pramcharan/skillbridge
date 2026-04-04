package com.skillbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.ai.AiScoringService;
import com.skillbridge.dto.ai.AiMatchResult;
import com.skillbridge.entity.Job;
import com.skillbridge.entity.Proposal;
import com.skillbridge.entity.User;
import com.skillbridge.entity.enums.JobCategory;
import com.skillbridge.entity.enums.Role;
import com.skillbridge.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiScoringService Unit Tests")
class AiScoringServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProposalRepository proposalRepository;

    private ObjectMapper objectMapper;

    private AiScoringService aiScoringService;

    private User freelancer;
    private Job job;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        aiScoringService = new AiScoringService();
        ReflectionTestUtils.setField(aiScoringService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(aiScoringService, "proposalRepository", proposalRepository);
        ReflectionTestUtils.setField(aiScoringService, "objectMapper", objectMapper);

        freelancer = new User();
        freelancer.setId(1L);
        freelancer.setEmail("dev@example.com");
        freelancer.setRole(Role.FREELANCER);
        freelancer.setSkills("Java,Spring Boot,MySQL,Docker");
        freelancer.setBio("5 years Java experience");

        job = new Job();
        job.setId(10L);
        job.setTitle("Build REST API");
        job.setDescription("Need Spring Boot developer with MySQL knowledge.");
        job.setCategory(JobCategory.TECHNOLOGY);
        job.setRequiredSkills("Java,Spring Boot,MySQL,Docker,Kubernetes");

        proposal = new Proposal();
        proposal.setId(100L);
        proposal.setFreelancer(freelancer);
        proposal.setJob(job);
    }

    @Test
    @DisplayName("should return AI-enriched result when Ollama is available")
    void scoreSync_ollamaAvailable_returnsResult() {
        String fakeOllamaResponse = """
            {
              "model": "llama2",
              "response": "{\\"finalScore\\": 87, \\"badge\\": \\"STRONG_MATCH\\", \\"explanation\\": \\"Candidate has all required skills.\\", \\"encouragement\\": \\"Great fit for this role!\\", \\"matchedSkills\\": [\\"Java\\", \\"Spring Boot\\", \\"MySQL\\"], \\"missingSkills\\": []}",
              "done": true
            }
            """;

        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(fakeOllamaResponse);

        AiMatchResult result = aiScoringService.scoreSync(freelancer, job, proposal);

        assertThat(result).isNotNull();
        assertThat(result.getFinalScore()).isEqualTo(0.87);
        assertThat(result.getBadge()).isEqualTo("STRONG_MATCH");
        assertThat(result.isAiEnriched()).isTrue();
        assertThat(result.getProvider()).isEqualTo("ollama");
        assertThat(result.getMatchedSkills()).isNotEmpty();

        verify(proposalRepository).save(proposal);
        assertThat(proposal.getAiMatchScore()).isEqualTo(0.87);
    }

    @Test
    @DisplayName("should fallback to keyword scoring when Ollama is unavailable")
    void scoreSync_ollamaUnavailable_fallbackScoring() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        AiMatchResult result = aiScoringService.scoreSync(freelancer, job, proposal);

        assertThat(result).isNotNull();
        assertThat(result.getFinalScore()).isEqualTo(0.8);
        assertThat(result.getBadge()).isEqualTo("STRONG_MATCH");
        assertThat(result.getProvider()).isEqualTo("KEYWORD_FALLBACK");
        assertThat(result.isAiEnriched()).isFalse();
        assertThat(result.getMatchedSkills()).contains("Java", "Spring Boot", "MySQL", "Docker");
        assertThat(result.getMissingSkills()).contains("Kubernetes");

        verify(proposalRepository).save(proposal);
        assertThat(proposal.getAiMatchScore()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("fallback score should reflect skill overlap ratio")
    void scoreSync_fallback_reflectsSkillOverlap() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("unavailable"));

        AiMatchResult result = aiScoringService.scoreSync(freelancer, job, proposal);
        double strongScore = result.getFinalScore();

        User noSkillFreelancer = new User();
        noSkillFreelancer.setId(99L);
        noSkillFreelancer.setSkills("Photoshop,Illustrator");
        noSkillFreelancer.setBio("Graphic designer");

        Proposal noSkillProposal = new Proposal();
        noSkillProposal.setId(200L);
        noSkillProposal.setFreelancer(noSkillFreelancer);
        noSkillProposal.setJob(job);

        AiMatchResult noMatchResult = aiScoringService.scoreSync(noSkillFreelancer, job, noSkillProposal);
        double weakScore = noMatchResult.getFinalScore();

        assertThat(strongScore).isGreaterThan(weakScore);
    }

    @Test
    @DisplayName("should save the score to the proposal")
    void scoreSync_savesScoreToProposal() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("unavailable"));

        AiMatchResult result = aiScoringService.scoreSync(freelancer, job, proposal);

        verify(proposalRepository).save(proposal);
        assertThat(proposal.getAiMatchScore()).isEqualTo(result.getFinalScore());
    }
}