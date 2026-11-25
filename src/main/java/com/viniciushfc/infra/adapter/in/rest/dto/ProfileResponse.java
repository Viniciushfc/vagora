package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.LlmModels;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id,
                              String name,
                              boolean hasGeminiApiKey,
                              boolean hasGroqApiKey,
                              boolean hasClaudeApiKey,
                              LlmProvider llmProvider,
                              boolean showBrowser,
                              boolean monitoringEnabled,
                              boolean autoCurriculum,
                              int pollMinMinutes,
                              int pollMaxMinutes,
                              List<LlmProvider> analysisChain,
                              List<LlmProvider> curriculumChain,
                              List<LlmProvider> chatChain,
                              String localLlmUrl,
                              String localLlmModel,
                              boolean hasLocalLlmApiKey,
                              boolean hasLocalLlm,
                              LlmModels models,
                              String curriculumName,
                              Integer curriculumSizeBytes,
                              List<LinkedAccountResponse> linkedAccounts) {
}
