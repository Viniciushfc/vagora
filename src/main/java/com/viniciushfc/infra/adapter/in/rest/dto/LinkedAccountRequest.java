package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.ExternalProvider;

public record LinkedAccountRequest(String username,
                                   String password,
                                   ExternalProvider externalProvider,
                                   String folder) {
}
