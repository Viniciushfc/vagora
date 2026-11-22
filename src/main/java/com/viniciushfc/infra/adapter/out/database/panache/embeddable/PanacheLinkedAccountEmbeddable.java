package com.viniciushfc.infra.adapter.out.database.panache.embeddable;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record PanacheLinkedAccountEmbeddable (UUID id,
                                              String username,
                                              String password,
                                              String externalProvider,
                                              String folder,
                                              String indeedFolder,
                                              String glassdoorFolder) {
}
