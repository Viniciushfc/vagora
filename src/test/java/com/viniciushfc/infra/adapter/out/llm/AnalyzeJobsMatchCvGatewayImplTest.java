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
import static org.mockito.Mockito.*;

class AnalyzeJobsMatchCvGatewayImplTest {

    private JobDetail job(String title) {
        return new JobDetail("link", title, "descrição de " + title, new CompanyDetails("ACME", "sobre"));
    }

    @Test
    void umaChamadaPorVagaNaMesmaOrdemComAsMarcacoesDoPrompt() {
        var gemini = mock(GeminiServiceImpl.class);
        var java = new CvMatchResult(true, 90, List.of("m"), "cv");
        var react = new CvMatchResult(false, 30, List.of("m"), null);
        when(gemini.analyze(argThat(p -> p != null && p.contains("Dev Java")), eq("chave"))).thenReturn(java);
        when(gemini.analyze(argThat(p -> p != null && p.contains("Dev React")), eq("chave"))).thenReturn(react);

        var results = new AnalyzeJobsMatchCvGatewayImpl(gemini)
                .executeAnalysis(List.of(job("Dev Java"), job("Dev React")), "meu curriculo", "chave");

        assertEquals(List.of(java, react), results);
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(gemini, times(2)).analyze(prompts.capture(), eq("chave"));
        String first = prompts.getAllValues().getFirst();
        assertTrue(first.contains("<vaga>") && first.contains("</vaga>"));
        assertTrue(first.contains("<curriculo>\nmeu curriculo\n</curriculo>"));
        assertTrue(first.contains("Empresa: ACME"));
        assertTrue(!first.contains("Dev React"), "cada prompt leva só uma vaga");
    }
}
