package com.skillbridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Getter
@Configuration
public class AiConfig {

    @Value("${app.ai.provider}")
    private String provider;

    // Ollama
    @Value("${app.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ai.ollama.model}")
    private String ollamaModel;

    @Value("${app.ai.ollama.timeout-seconds}")
    private int ollamaTimeoutSeconds;

    // OpenAI
    @Value("${app.ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${app.ai.openai.model}")
    private String openAiModel;

    @Value("${app.ai.openai.api-key:not-set}")
    private String openAiApiKey;
}