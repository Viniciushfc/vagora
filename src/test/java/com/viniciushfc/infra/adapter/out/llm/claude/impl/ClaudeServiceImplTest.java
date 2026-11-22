package com.viniciushfc.infra.adapter.out.llm.claude.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void comChaveDaApiChamaAApiSemPrecisarDoClaudeCode() throws Exception {
        var objectMapper = new ObjectMapper();
        var api = mock(ClaudeApi.class);
        when(api.adaptCurriculum("prompt", "sk-ant")).thenReturn("cv");
        var service = new ClaudeServiceImpl(new CvMatchMapper(objectMapper), objectMapper, "sonnet", api);

        assertEquals("cv", service.adaptCurriculum("prompt", "sk-ant"));
        service.analyze("prompt", 1, "sk-ant");
        verify(api).analyze("prompt", 1, "sk-ant");
    }
}
