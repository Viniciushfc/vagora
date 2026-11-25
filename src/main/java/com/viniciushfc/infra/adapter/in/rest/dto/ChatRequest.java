package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.vo.ChatMessage;

import java.util.List;

public record ChatRequest(List<ChatMessage> messages) {
}
