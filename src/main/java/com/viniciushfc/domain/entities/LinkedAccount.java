package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;

import java.util.UUID;

public record LinkedAccount(UUID id,
                            String username,
                            String password,
                            ExternalProvider externalProvider,
                            String folder,
                            String indeedFolder,
                            String glassdoorFolder) {

    public static LinkedAccount create(String username, String password, ExternalProvider externalProvider, String folder) {
        return new LinkedAccount(UUID.randomUUID(), username, password, externalProvider, folder, null, null);
    }

    public String folder(AlertSource source) {
        return switch (source) {
            case INDEED -> indeedFolder;
            case GLASSDOOR -> glassdoorFolder;
            default -> folder;
        };
    }

    public LinkedAccount withFolder(AlertSource source, String folder) {
        return switch (source) {
            case INDEED -> new LinkedAccount(id, username, password, externalProvider, this.folder, folder, glassdoorFolder);
            case GLASSDOOR -> new LinkedAccount(id, username, password, externalProvider, this.folder, indeedFolder, folder);
            default -> new LinkedAccount(id, username, password, externalProvider, folder, indeedFolder, glassdoorFolder);
        };
    }
}
