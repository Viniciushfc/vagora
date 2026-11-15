package com.viniciushfc.infra.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id,
                                  String name,
                                  boolean hasGeminiApiKey,
                                  boolean showBrowser,
                                  boolean monitoringEnabled,
                                  String curriculumName,
                                  Integer curriculumSizeBytes,
                                  List<LinkedAccountResponse> linkedAccounts) {
}
