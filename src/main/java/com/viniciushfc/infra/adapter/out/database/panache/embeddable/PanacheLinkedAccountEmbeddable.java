package com.viniciushfc.infra.adapter.out.database.panache.embeddable;

import com.viniciushfc.domain.enums.impl.ExternalProvider;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record PanacheLinkedAccountEmbeddable (UUID id,
                                              String username,
                                              String password,
                                              ExternalProvider externalProvider) {
}
