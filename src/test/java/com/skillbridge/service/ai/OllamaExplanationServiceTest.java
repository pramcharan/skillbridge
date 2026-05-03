package com.skillbridge.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillbridge.config.AiConfig;
import com.skillbridge.dto.ai.AiMatchRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaExplanationServiceTest {

    @Mock
    private AiConfig aiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;
    private int port;
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";

    private OllamaExplanationService service;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/generate", exchange -> {
            try (var in = exchange.getRequestBody()) {
                in.readAllBytes();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        port = server.getAddress().getPort();

        when(aiConfig.getOllamaBaseUrl()).thenReturn("http://127.0.0.1:" + port);
        when(aiConfig.getOllamaModel()).thenReturn("llama-test");
        when(aiConfig.getOllamaTimeoutSeconds()).thenReturn(30);

        service = new OllamaExplanationService(aiConfig, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getProviderName_returnsOllama() {
        assertThat(service.getProviderName()).isEqualTo("ollama");
    }

    @Test
    void enrichExplanation_success() throws Exception {
        String inner = "{\"explanation\":\"Great fit for backend work.\",\"encouragement\":\"Highlight SQL.\"}";
        responseBody = ollamaGenerateWrapper(inner);

        AiMatchRequest req = fullRequest();
        assertThat(service.enrichExplanation(req)).isEqualTo("Great fit for backend work. Highlight SQL.");
    }

    @Test
    void enrichExplanation_encouragementNull_returnsExplanationOnly() throws Exception {
        String inner = "{\"explanation\":\"Adequate match.\",\"encouragement\":null}";
        responseBody = ollamaGenerateWrapper(inner);

        assertThat(service.enrichExplanation(fullRequest())).isEqualTo("Adequate match.");
    }

    @Test
    void enrichExplanation_nullBio_usesNotProvided() throws Exception {
        String inner = "{\"explanation\":\"Ok.\",\"encouragement\":null}";
        responseBody = ollamaGenerateWrapper(inner);

        AiMatchRequest req = fullRequest();
        req.setFreelancerBio(null);
        assertThat(service.enrichExplanation(req)).isEqualTo("Ok.");
    }

    @Test
    void enrichExplanation_httpError_returnsNull() {
        responseStatus = 502;
        responseBody = "bad gateway";
        assertThat(service.enrichExplanation(fullRequest())).isNull();
    }

    @Test
    void enrichExplanation_responseWithoutJsonBlock_returnsNull() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("response", "no braces here");
        responseBody = objectMapper.writeValueAsString(root);

        assertThat(service.enrichExplanation(fullRequest())).isNull();
    }

    @Test
    void enrichExplanation_invalidInnerJson_returnsNull() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("response", "{ not valid json }");
        responseBody = objectMapper.writeValueAsString(root);

        assertThat(service.enrichExplanation(fullRequest())).isNull();
    }

    private String ollamaGenerateWrapper(String modelSays) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("response", modelSays);
        return objectMapper.writeValueAsString(root);
    }

    private static AiMatchRequest fullRequest() {
        return AiMatchRequest.builder()
                .freelancerName("Alex")
                .freelancerBio("Backend dev")
                .freelancerSkills(List.of("Java"))
                .freelancerRate(80.0)
                .freelancerAvailability("FULL_TIME")
                .jobTitle("API Engineer")
                .jobDescription("Need REST ".repeat(50))
                .requiredSkills(List.of("Java", "Spring"))
                .jobBudget(5000.0)
                .preCalculatedScore(82.0)
                .preCalculatedBadge("STRONG")
                .matchedSkills(List.of("Java"))
                .missingSkills(List.of("Spring"))
                .build();
    }
}
