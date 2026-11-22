package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.LocalLlm;

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
                      Boolean autoCurriculum,
                      Integer pollMinMinutes,
                      Integer pollMaxMinutes,
                      List<LlmProvider> analysisChain,
                      List<LlmProvider> curriculumChain,
                      LocalLlm localLlm,
                      String claudeApiKey) {

    public static final int DEFAULT_POLL_MIN_MINUTES = 15;
    public static final int DEFAULT_POLL_MAX_MINUTES = 25;

    public static Profile create(String name,
                                 String geminiApiKey,
                                 String groqApiKey,
                                 LlmProvider llmProvider,
                                 Attachment curriculum) {
        return new Profile(UUID.randomUUID(), name, geminiApiKey, curriculum, new ArrayList<>(),
                false, true, groqApiKey, llmProvider, false, null, null, null, null, null, null);
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

    public int pollMin() {
        return pollMinMinutes == null ? DEFAULT_POLL_MIN_MINUTES : pollMinMinutes;
    }

    public int pollMax() {
        return pollMaxMinutes == null ? DEFAULT_POLL_MAX_MINUTES : pollMaxMinutes;
    }

    public LlmProvider primaryLlm() {
        return llmProvider == null ? LlmProvider.GEMINI : llmProvider;
    }

    public String apiKey(LlmProvider provider) {
        return switch (provider) {
            case GEMINI -> geminiApiKey;
            case GROQ -> groqApiKey;
            case CLAUDE -> claudeApiKey;
            case LOCAL -> localLlm == null ? null : localLlm.apiKey();
        };
    }

    public List<LlmProvider> llmProviders() {
        return available(defaultOrder());
    }

    public List<LlmProvider> analysisOrder() {
        return analysisChain == null ? defaultOrder() : analysisChain;
    }

    public List<LlmProvider> curriculumOrder() {
        return curriculumChain == null ? defaultOrder() : curriculumChain;
    }

    public List<LlmProvider> analysisProviders() {
        return available(analysisOrder());
    }

    public List<LlmProvider> curriculumProviders() {
        return available(curriculumOrder());
    }

    private List<LlmProvider> defaultOrder() {
        LlmProvider primary = primaryLlm();
        Stream<LlmProvider> others = Arrays.stream(LlmProvider.values())
                .filter(provider -> provider != primary);
        return Stream.concat(Stream.of(primary), others).toList();
    }

    private List<LlmProvider> available(List<LlmProvider> order) {
        return order.stream()
                .filter(this::hasApiKey)
                .toList();
    }

    private boolean hasApiKey(LlmProvider provider) {
        if (provider == LlmProvider.CLAUDE) {
            return (claudeApiKey != null && !claudeApiKey.isBlank()) || findLinkedAccount(ExternalProvider.CLAUDE) != null;
        }
        if (provider == LlmProvider.LOCAL) {
            return localLlm != null && localLlm.configured();
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
