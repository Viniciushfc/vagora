package com.viniciushfc.infra.adapter.out.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.vo.ScraperActivity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobDetailsGatewayImplTest {

    private final ObjectMapper json = new ObjectMapper();

    private ScraperActivity apply(ScraperActivity activity, String event) throws Exception {
        return JobDetailsGatewayImpl.applyEvent(json.readTree(event)).apply(activity);
    }

    @Test
    void traduzOsCodigosDeSaidaSemTextoTecnico() {
        assertEquals(ErrorCode.LINKEDIN_CHECKPOINT, JobDetailsGatewayImpl.failure(3).code());
        assertEquals(ErrorCode.LINKEDIN_LOGIN_FAILED, JobDetailsGatewayImpl.failure(4).code());
        assertEquals(ErrorCode.LINKEDIN_PAGE_UNREADABLE, JobDetailsGatewayImpl.failure(5).code());
        assertEquals(ErrorCode.SCRAPER_FAILED, JobDetailsGatewayImpl.failure(1).code());
        for (int code : new int[]{1, 3, 4, 5}) {
            String message = JobDetailsGatewayImpl.failure(code).getMessage();
            assertFalse(message.matches(".*(Timeout|Locator|Exception|código \\d).*"), message);
        }
    }

    @Test
    void eventosDoScraperViramOEstadoDaPagina() throws Exception {
        var activity = apply(ScraperActivity.idle(), "{\"event\":\"start\",\"total\":2}");
        assertTrue(activity.running());

        activity = apply(activity, "{\"event\":\"job\",\"index\":1,\"total\":2}");
        assertEquals(1, activity.jobIndex());
        assertEquals("Abrindo a vaga 1 de 2", activity.events().getFirst().message());

        activity = apply(activity, "{\"event\":\"closed\",\"index\":1}");
        assertEquals("Vaga 1 não aceita mais candidaturas: pulada", activity.events().getFirst().message());

        activity = apply(activity, "{\"event\":\"done\",\"count\":1}");
        assertFalse(activity.running());
        assertNull(activity.failure());

        activity = activity.failed("Explicação amigável", true);
        assertEquals("Explicação amigável", activity.failure());
        assertTrue(activity.events().getFirst().problem());
    }
}
