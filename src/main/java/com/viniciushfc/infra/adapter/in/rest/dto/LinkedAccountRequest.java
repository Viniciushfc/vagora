package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.impl.ExternalProvider;

/**
 * {@code folder} só vale para contas de email e é opcional: sem ela, a pasta é detectada.
 */
public record LinkedAccountRequest(String username,
                                   String password,
                                   ExternalProvider externalProvider,
                                   String folder) {
}
