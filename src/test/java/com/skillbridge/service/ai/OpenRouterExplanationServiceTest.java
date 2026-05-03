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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenRouterExplanationServiceTest {

    @Mock
    private AiConfig aiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;
    private int port;
    private final AtomicInteger callSeq = new AtomicInteger();
    private volatile String firstBody = "";
    private volatile String secondBody = "";
    private volatile int firstStatus = 200;
    private volatile int secondStatus = 200;

    private OpenRouterExplanationService service;

    @BeforeEach
    void setUp() throws Exception {
        callSeq.set(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try (var in = exchange.getRequestBody()) {
                in.readAllBytes();
            }
            int n = callSeq.getAndIncrement();
            int status = n == 0 ? firstStatus : secondStatus;
            String body = n == 0 ? firstBody : secondBody;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        port = server.getAddress().getPort();

        when(aiConfig.getOpenrouterBaseUrl()).thenReturn("http://127.0.0.1:" + port);
        when(aiConfig.getOpenrouterApiKey()).thenReturn("key-test");
        when(aiConfig.getOpenrouterPrimaryModel()).thenReturn("primary-model");
        when(aiConfig.getOpenrouterBackupModel()).thenReturn("backup-model");

        service = new OpenRouterExplanationService(aiConfig, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getProviderName_returnsOpenrouter() {
        assertThat(service.getProviderName()).isEqualTo("openrouter");
    }

    @Test
    void enrichExplanation_primaryModelSucceeds() throws Exception {
        firstBody = routerChoicesWrapper("{\"explanation\":\"Good.\",\"encouragement\":\"Tip.\"}");
        AiMatchRequest req = baseRequest();
        assertThat(service.enrichExplanation(req)).isEqualTo("Good. Tip.");
        assertThat(callSeq.get()).isEqualTo(1);
    }

    @Test
    void enrichExplanation_primaryFailsThenBackupSucceeds() throws Exception {
        firstStatus = 500;
        firstBody = "primary down";
        secondBody = routerChoicesWrapper("{\"explanation\":\"Backup ok.\",\"encouragement\":null}");
        assertThat(service.enrichExplanation(baseRequest())).isEqualTo("Backup ok.");
        assertThat(callSeq.get()).isEqualTo(2);
    }

    @Test
    void enrichExplanation_bothModelsFail_returnsNull() {
        firstStatus = 500;
        secondStatus = 500;
        firstBody = "err1";
        secondBody = "err2";
        assertThat(service.enrichExplanation(baseRequest())).isNull();
        assertThat(callSeq.get()).isEqualTo(2);
    }

    @Test
    void enrichExplanation_invalidJsonInContent_returnsNull() throws Exception {
        firstBody = routerChoicesWrapper("not json");
        assertThat(service.enrichExplanation(baseRequest())).isNull();
    }

    private String routerChoicesWrapper(String messageContent) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode choices = root.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.putObject("message").put("content", messageContent);
        return objectMapper.writeValueAsString(root);
    }

    private static AiMatchRequest baseRequest() {
        return AiMatchRequest.builder()
                .freelancerSkills(List.of("Go"))
                .requiredSkills(List.of("Go", "gRPC"))
                .preCalculatedScore(75.0)
                .matchedSkills(List.of("Go"))
                .missingSkills(List.of("gRPC"))
                .build();
    }
}
