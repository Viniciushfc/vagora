package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.errors.ApiException;
import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeminiServiceImplTest {

    @Test
    void cotaEsgotadaEsperaOTempoPedidoPeloGemini() {
        RateLimitedException e = GeminiServiceImpl.quotaExceeded("Quota exceeded. Please retry in 7h23m28.46s.");

        assertEquals(Duration.ofHours(7).plusMinutes(23).plusSeconds(29), e.retryAfter());
        assertEquals(ErrorCode.LLM_QUOTA_EXCEEDED, e.code());
    }

    @Test
    void cotaEsgotadaSemTempoInformadoEspera30Minutos() {
        assertEquals(Duration.ofMinutes(30), GeminiServiceImpl.quotaExceeded("Too Many Requests").retryAfter());
    }

    private static ErrorCode codeOf(int status, String message) {
        return ((DomainException) GeminiServiceImpl.translate(new ApiException(status, "STATUS", message))).code();
    }

    @Test
    void soChaveInvalidaViraChaveRecusada() {
        assertEquals(ErrorCode.LLM_KEY_REJECTED, codeOf(400, "API key not valid. Please pass a valid API key."));
        assertEquals(ErrorCode.LLM_KEY_REJECTED, codeOf(403, "Permission denied"));
        assertNull(codeOf(400, "Request contains an invalid argument."), "outro 400 não culpa a chave");
    }
}
