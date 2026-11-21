package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.LlmProvider;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id,
                              String name,
                              boolean hasGeminiApiKey,
                              boolean hasGroqApiKey,
                              LlmProvider llmProvider,
                              boolean showBrowser,
                              boolean monitoringEnabled,
                              boolean autoCurriculum,
                              int pollMinMinutes,
                              int pollMaxMinutes,
                              List<LlmProvider> analysisChain,
                              List<LlmProvider> curriculumChain,
                              String curriculumName,
                              Integer curriculumSizeBytes,
                              List<LinkedAccountResponse> linkedAccounts) {
}
