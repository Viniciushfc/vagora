package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.impl.ExternalProvider;

import java.util.UUID;

public record LinkedAccount(UUID id,
                            String username,
                            String password,
                            ExternalProvider externalProvider,
                            String folder) {

    public static LinkedAccount create(String username, String password, ExternalProvider externalProvider, String folder) {
        return new LinkedAccount(UUID.randomUUID(), username, password, externalProvider, folder);
    }

    public LinkedAccount withFolder(String folder) {
        return new LinkedAccount(id, username, password, externalProvider, folder);
    }
}
