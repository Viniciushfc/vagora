package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;

import java.util.UUID;

public record LinkedAccount(UUID id,
                            String username,
                            String password,
                            ExternalProvider externalProvider,
                            String folder,
                            String indeedFolder) {

    public static LinkedAccount create(String username, String password, ExternalProvider externalProvider, String folder) {
        return new LinkedAccount(UUID.randomUUID(), username, password, externalProvider, folder, null);
    }

    public String folder(AlertSource source) {
        return source == AlertSource.INDEED ? indeedFolder : folder;
    }

    public LinkedAccount withFolder(AlertSource source, String folder) {
        return source == AlertSource.INDEED
                ? new LinkedAccount(id, username, password, externalProvider, this.folder, folder)
                : new LinkedAccount(id, username, password, externalProvider, folder, indeedFolder);
    }
}
