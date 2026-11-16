package com.viniciushfc.infra.adapter.out.llm;

import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.gateway.AnalyzeJobsMatchCvGatewayImpl;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeJobsMatchCvGatewayImplTest {

    private JobDetail job(String title) {
        return new JobDetail("link", title, "descrição de " + title, new CompanyDetails("ACME", "sobre"));
    }

    @Test
    void mandaTodasAsVagasNumPromptComUmIdPorVaga() {
        var gemini = mock(GeminiServiceImpl.class);
        var results = List.of(new CvMatchResult(true, 90, List.of("m"), null), new CvMatchResult(false, 20, List.of("n"), null));
        when(gemini.analyze(anyString(), eq(2), eq("chave"))).thenReturn(results);

        assertEquals(results, new AnalyzeJobsMatchCvGatewayImpl(gemini)
                .analyze(List.of(job("Dev Java"), job("Dev Go")), "meu curriculo", "chave"));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).analyze(prompt.capture(), eq(2), eq("chave"));
        assertTrue(prompt.getValue().contains("<vaga id=\"1\">\nTítulo: Dev Java"));
        assertTrue(prompt.getValue().contains("<vaga id=\"2\">\nTítulo: Dev Go"));
        assertTrue(prompt.getValue().contains("<curriculo>\nmeu curriculo\n</curriculo>"));
    }
}
