package com.viniciushfc.domain.vo;

public record Attachment(String name,
                         String contentType,
                         byte[] content) {
}
