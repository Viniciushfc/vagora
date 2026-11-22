package com.viniciushfc.domain.vo;

import java.net.URI;

public record LocalLlm(String url, String model, String apiKey) {

    public boolean configured() {
        return url != null && !url.isBlank() && model != null && !model.isBlank();
    }

    public URI chatCompletions() {
        String base = url.strip().replaceAll("/+$", "");
        return URI.create(base.endsWith("/chat/completions") ? base : base + "/chat/completions");
    }
}
