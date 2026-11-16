package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.ExternalProvider;

import java.util.UUID;

public record LinkedAccountResponse(UUID id,
                                    String username,
                                    ExternalProvider externalProvider,
                                    String folder) {
}
