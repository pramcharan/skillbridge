package com.skillbridge.service.ai;

import com.skillbridge.config.AiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiExplanationFactoryTest {

    @Mock
    private AiConfig aiConfig;

    @Mock
    private AiExplanationService ollamaService;

    @Mock
    private AiExplanationService openaiService;

    private AiExplanationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AiExplanationFactory(
                aiConfig,
                Map.of(
                        "ollama", ollamaService,
                        "openai", openaiService
                )
        );
    }

    @Test
    void getProvider_returnsConfiguredProvider_whenPresent() {
        when(aiConfig.getProvider()).thenReturn("openai");

        AiExplanationService result = factory.getProvider();

        assertSame(openaiService, result);
    }

    @Test
    void getProvider_fallsBackToOllama_whenConfiguredProviderMissing() {
        when(aiConfig.getProvider()).thenReturn("unknown");

        AiExplanationService result = factory.getProvider();

        assertSame(ollamaService, result);
    }

    @Test
    void getProvider_returnsOllama_whenProviderIsOllama() {
        when(aiConfig.getProvider()).thenReturn("ollama");

        AiExplanationService result = factory.getProvider();

        assertSame(ollamaService, result);
    }
}