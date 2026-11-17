package com.viniciushfc.infra.adapter.out.database.panache.embeddable;

import com.viniciushfc.domain.enums.ExternalProvider;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

@Embeddable
public record PanacheLinkedAccountEmbeddable (UUID id,
                                              String username,
                                              String password,
                                              @Enumerated(EnumType.STRING) ExternalProvider externalProvider,
                                              String folder,
                                              String indeedFolder) {
}
