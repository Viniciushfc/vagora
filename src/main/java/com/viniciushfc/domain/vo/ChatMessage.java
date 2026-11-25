package com.viniciushfc.domain.vo;

public record ChatMessage(String role,
                          String text) {

    public boolean fromUser() {
        return "user".equals(role);
    }
}
