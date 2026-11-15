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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeJobsMatchCvGatewayImplTest {

    private JobDetail job(String title) {
        return new JobDetail("link", title, "descrição de " + title, new CompanyDetails("ACME", "sobre"));
    }

    @Test
    void mandaUmaVagaPorPromptComAsMarcacoesEsperadas() {
        var gemini = mock(GeminiServiceImpl.class);
        var result = new CvMatchResult(true, 90, List.of("m"), "cv");
        when(gemini.analyze(argThat(p -> p != null && p.contains("Dev Java")), eq("chave"))).thenReturn(result);

        assertEquals(result, new AnalyzeJobsMatchCvGatewayImpl(gemini).analyze(job("Dev Java"), "meu curriculo", "chave"));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).analyze(prompt.capture(), eq("chave"));
        assertTrue(prompt.getValue().contains("<vaga>") && prompt.getValue().contains("</vaga>"));
        assertTrue(prompt.getValue().contains("<curriculo>\nmeu curriculo\n</curriculo>"));
        assertTrue(prompt.getValue().contains("Empresa: ACME"));
    }
}
