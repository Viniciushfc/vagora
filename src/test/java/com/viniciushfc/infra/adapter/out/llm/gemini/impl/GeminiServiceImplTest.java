package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiServiceImplTest {

    @Test
    void cotaEsgotadaEsperaOTempoPedidoPeloGemini() {
        RateLimitedException e = GeminiServiceImpl.quotaExceeded("Quota exceeded. Please retry in 7h23m28.46s.");

        assertEquals(Duration.ofHours(7).plusMinutes(23).plusSeconds(29), e.retryAfter());
        assertEquals(ErrorCode.GEMINI_QUOTA_EXCEEDED, e.code());
    }

    @Test
    void cotaEsgotadaSemTempoInformadoEspera30Minutos() {
        assertEquals(Duration.ofMinutes(30), GeminiServiceImpl.quotaExceeded("Too Many Requests").retryAfter());
    }
}
