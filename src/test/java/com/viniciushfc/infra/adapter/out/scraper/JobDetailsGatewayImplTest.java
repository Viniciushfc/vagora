package com.viniciushfc.infra.adapter.out.scraper;

import com.viniciushfc.domain.exceptions.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobDetailsGatewayImplTest {

    @Test
    void traduzOsCodigosDeSaidaDoScraper() {
        assertEquals(ErrorCode.LINKEDIN_CHECKPOINT, JobDetailsGatewayImpl.failure(3).code());
        assertEquals(ErrorCode.LINKEDIN_LOGIN_FAILED, JobDetailsGatewayImpl.failure(4).code());
        assertTrue(JobDetailsGatewayImpl.failure(1).getMessage().contains("código 1"));
    }
}
