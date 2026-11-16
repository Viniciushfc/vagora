package com.viniciushfc.infra.adapter.out.database.panache.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PanacheAttachmentEmbeddable(String name,
                                          String contentType,
                                          @Column(length = PanacheAttachmentEmbeddable.MAX_CONTENT_LENGTH) byte[] content) {

    public static final int MAX_CONTENT_LENGTH = 20 * 1024 * 1024;
}
