package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.impl.ExternalProvider;

import java.util.UUID;

public record LinkedAccount(UUID id,
                            String username,
                            String password,
                            ExternalProvider externalProvider) {
}
