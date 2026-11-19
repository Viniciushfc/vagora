package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.Attachment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public record Profile(UUID id,
                      String name,
                      String geminiApiKey,
                      Attachment curriculum,
                      List<LinkedAccount> linkedAccounts,
                      Boolean showBrowser,
                      Boolean monitoringEnabled,
                      String groqApiKey,
                      LlmProvider llmProvider,
                      Boolean autoCurriculum) {

    public static Profile create(String name,
                                 String geminiApiKey,
                                 String groqApiKey,
                                 LlmProvider llmProvider,
                                 Attachment curriculum) {
        return new Profile(UUID.randomUUID(), name, geminiApiKey, curriculum, new ArrayList<>(),
                false, true, groqApiKey, llmProvider, false);
    }

    public boolean isBrowserVisible() {
        return Boolean.TRUE.equals(showBrowser);
    }

    public boolean isMonitoringEnabled() {
        return !Boolean.FALSE.equals(monitoringEnabled);
    }

    public boolean isAutoCurriculum() {
        return Boolean.TRUE.equals(autoCurriculum);
    }

    public LlmProvider primaryLlm() {
        return llmProvider == null ? LlmProvider.GEMINI : llmProvider;
    }

    public String apiKey(LlmProvider provider) {
        return switch (provider) {
            case GEMINI -> geminiApiKey;
            case GROQ -> groqApiKey;
            case CLAUDE -> null;
        };
    }

    public List<LlmProvider> llmProviders() {
        LlmProvider primary = primaryLlm();
        Stream<LlmProvider> others = Arrays.stream(LlmProvider.values())
                .filter(provider -> provider != primary);

        return Stream.concat(Stream.of(primary), others)
                .filter(this::hasApiKey)
                .toList();
    }

    private boolean hasApiKey(LlmProvider provider) {
        if (provider == LlmProvider.CLAUDE) {
            return findLinkedAccount(ExternalProvider.CLAUDE) != null;
        }
        String key = apiKey(provider);
        return key != null && !key.isBlank();
    }

    public LinkedAccount findLinkedAccount(ExternalProvider provider) {
        if (linkedAccounts == null) {
            return null;
        }
        return linkedAccounts.stream()
                .filter(account -> account.externalProvider() == provider)
                .findFirst()
                .orElse(null);
    }
}
