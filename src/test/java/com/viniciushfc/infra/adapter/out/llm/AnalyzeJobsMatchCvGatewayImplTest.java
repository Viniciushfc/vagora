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
import static org.mockito.Mockito.*;

class AnalyzeJobsMatchCvGatewayImplTest {

    @Test
    void enviaVagasECurriculoNoPrompt() {
        var gemini = mock(GeminiServiceImpl.class);
        var results = List.of(new CvMatchResult(true, 90, List.of("m"), "cv"));
        when(gemini.genareteContentText(anyString())).thenReturn(results);
        var job = new JobDetail("link", "Dev Java", "desc vaga", new CompanyDetails("ACME", "sobre"));

        var returned = new AnalyzeJobsMatchCvGatewayImpl(gemini).executeAnalysis(List.of(job), "meu curriculo");

        assertEquals(results, returned);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).genareteContentText(prompt.capture());
        assertTrue(prompt.getValue().contains("Dev Java"));
        assertTrue(prompt.getValue().contains("desc vaga"));
        assertTrue(prompt.getValue().contains("meu curriculo"));
    }
}
