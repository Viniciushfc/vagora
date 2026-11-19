package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.ClaudeLoginMethod;

public record ClaudeLoginRequest(ClaudeLoginMethod method) {
}
