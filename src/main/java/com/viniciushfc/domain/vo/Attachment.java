package com.viniciushfc.domain.vo;

public record Attachment(String name,
                         String contentType,
                         byte[] content) {
    public Attachment(String name, String contentType, byte[] content) {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }
}
