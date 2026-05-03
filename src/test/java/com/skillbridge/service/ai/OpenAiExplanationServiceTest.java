package com.skillbridge.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
class OpenAiExplanationServiceTest {

    @Mock
    private AiConfig aiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;
    private int port;
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";

    private OpenAiExplanationService service;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
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

        when(aiConfig.getOpenAiBaseUrl()).thenReturn("http://127.0.0.1:" + port);
        when(aiConfig.getOpenAiModel()).thenReturn("gpt-test");
        when(aiConfig.getOpenAiApiKey()).thenReturn("sk-test");

        service = new OpenAiExplanationService(aiConfig, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getProviderName_returnsOpenai() {
        assertThat(service.getProviderName()).isEqualTo("openai");
    }

    @Test
    void enrichExplanation_success_combinesExplanationAndEncouragement() throws Exception {
        String inner = "{\"explanation\":\"Strong Java overlap.\",\"encouragement\":\"Add Kubernetes experience.\"}";
        responseBody = openAiChoicesWrapper(inner);

        AiMatchRequest req = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java"))
                .requiredSkills(List.of("Java", "Spring"))
                .preCalculatedScore(80.0)
                .matchedSkills(List.of("Java"))
                .missingSkills(List.of("Spring"))
                .jobDescription("Build an API ".repeat(30))
                .build();

        assertThat(service.enrichExplanation(req))
                .isEqualTo("Strong Java overlap. Add Kubernetes experience.");
    }

    @Test
    void enrichExplanation_encouragementNull_returnsExplanationOnly() throws Exception {
        String inner = "{\"explanation\":\"Solid match.\",\"encouragement\":null}";
        responseBody = openAiChoicesWrapper(inner);

        assertThat(service.enrichExplanation(baseRequest())).isEqualTo("Solid match.");
    }

    @Test
    void enrichExplanation_nullJobDescription_usesEmptySnippet() throws Exception {
        String inner = "{\"explanation\":\"Ok.\",\"encouragement\":null}";
        responseBody = openAiChoicesWrapper(inner);

        AiMatchRequest req = AiMatchRequest.builder()
                .freelancerSkills(List.of("Java"))
                .requiredSkills(List.of("Java"))
                .preCalculatedScore(70.0)
                .matchedSkills(List.of("Java"))
                .missingSkills(List.of())
                .jobDescription(null)
                .build();

        assertThat(service.enrichExplanation(req)).isEqualTo("Ok.");
    }

    @Test
    void enrichExplanation_non200_returnsNull() {
        responseStatus = 503;
        responseBody = "unavailable";
        assertThat(service.enrichExplanation(baseRequest())).isNull();
    }

    @Test
    void enrichExplanation_contentWithoutJson_returnsNull() throws Exception {
        responseBody = openAiChoicesWrapper("no json here");
        assertThat(service.enrichExplanation(baseRequest())).isNull();
    }

    private String openAiChoicesWrapper(String messageContent) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.putObject("message").put("content", messageContent);
        return objectMapper.writeValueAsString(root);
    }

    private static AiMatchRequest baseRequest() {
        return AiMatchRequest.builder()
                .freelancerSkills(List.of("Java"))
                .requiredSkills(List.of("Java"))
                .preCalculatedScore(70.0)
                .matchedSkills(List.of("Java"))
                .missingSkills(List.of())
                .jobDescription("Short desc")
                .build();
    }
}
