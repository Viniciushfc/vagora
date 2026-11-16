package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.vo.Attachment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Profile(UUID id,
                          String name,
                          String geminiApiKey,
                          Attachment curriculum,
                          List<LinkedAccount> linkedAccounts,
                          Boolean showBrowser,
                          Boolean monitoringEnabled) {

    public static Profile create(String name, String geminiApiKey, Attachment curriculum) {
        return new Profile(UUID.randomUUID(), name, geminiApiKey, curriculum, new ArrayList<>(), false, true);
    }

    public boolean isBrowserVisible() {
        return Boolean.TRUE.equals(showBrowser);
    }

    public boolean isMonitoringEnabled() {
        return !Boolean.FALSE.equals(monitoringEnabled);
    }

    public LinkedAccount findLinkedAccount(ExternalProvider provider) {
        if (linkedAccounts == null) {
            return null;
        }
        return linkedAccounts.stream()
                .filter(account -> account.externalProvider() == provider)
                .findFirst()
                .orElse(null);
    }
}
