package com.viniciushfc.infra.adapter.out.database.panache.embeddable;

import jakarta.persistence.Embeddable;

@Embeddable
public record PanacheAttachmentEmbeddable(String name,
                                          String contentType,
                                          byte[] content) {

}
