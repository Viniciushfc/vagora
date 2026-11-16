package com.viniciushfc.domain.enums;

public enum ExternalProvider {
    LINKEDIN("LinkedIn"),
    EMAIL("Email");

    public final String label;

    ExternalProvider(String label) {
        this.label = label;
    }
}
