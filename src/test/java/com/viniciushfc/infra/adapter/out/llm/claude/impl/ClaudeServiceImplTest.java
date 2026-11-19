package com.viniciushfc.infra.adapter.out.llm.claude.impl;

import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ClaudeServiceImplTest {

    @Test
    void semLoginPedeParaEntrarDeNovoEmContas() {
        var error = assertInstanceOf(ExternalServiceException.class,
                ClaudeServiceImpl.translate(0, "Not logged in · Please run /login"));
        assertEquals(ErrorCode.CLAUDE_NOT_CONNECTED, error.code());
    }

    @Test
    void limiteDaAssinaturaViraCotaParaAReservaAssumir() {
        assertInstanceOf(RateLimitedException.class, ClaudeServiceImpl.translate(429, "rate_limit_error"));
        assertInstanceOf(RateLimitedException.class, ClaudeServiceImpl.translate(0, "You've hit your usage limit"));
    }
}
