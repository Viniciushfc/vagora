package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.impl.ExternalProvider;

import java.util.UUID;

public record LinkedAccount(UUID id,
                            String username,
                            String password,
                            ExternalProvider externalProvider) {

    public static LinkedAccount create(String username, String password, ExternalProvider externalProvider) {
        return new LinkedAccount(UUID.randomUUID(), username, password, externalProvider);
    }
}
