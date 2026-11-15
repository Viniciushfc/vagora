package com.viniciushfc.domain.enums.impl;

/** Site de uma conta conectada. O perfil tem no máximo uma conta por provider. */
public enum ExternalProvider {
    LINKEDIN("LinkedIn"),
    EMAIL("Email");

    public final String label;

    ExternalProvider(String label) {
        this.label = label;
    }
}
