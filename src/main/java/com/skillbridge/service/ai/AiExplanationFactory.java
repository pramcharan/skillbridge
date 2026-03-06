package com.skillbridge.service.ai;

import com.skillbridge.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiExplanationFactory {

    private final AiConfig aiConfig;
    // Spring injects all AiExplanationService beans by name
    private final Map<String, AiExplanationService> providers;

    public AiExplanationService getProvider() {
        String providerName = aiConfig.getProvider();
        AiExplanationService service = providers.get(providerName);

        if (service == null) {
            log.warn("AI provider '{}' not found, falling back to ollama", providerName);
            return providers.get("ollama");
        }

        log.debug("Using AI provider: {}", providerName);
        return service;
    }
}