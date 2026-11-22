package com.viniciushfc.infra.adapter.out.llm;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.claude.impl.ClaudeServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.gateway.AnalyzeJobsMatchCvGatewayImpl;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import com.viniciushfc.infra.adapter.out.llm.groq.impl.GroqServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalyzeJobsMatchCvGatewayImplTest {

    private final GeminiServiceImpl gemini = mock(GeminiServiceImpl.class);
    private final GroqServiceImpl groq = mock(GroqServiceImpl.class);
    private final AnalyzeJobsMatchCvGatewayImpl gateway = new AnalyzeJobsMatchCvGatewayImpl(gemini, groq, mock(ClaudeServiceImpl.class), null, null);

    private JobDetail job(String title) {
        return new JobDetail("link", title, "descrição de " + title, new CompanyDetails("ACME", "sobre"));
    }

    private Profile profile(String geminiKey, String groqKey, LlmProvider primary) {
        return new Profile(null, "F", geminiKey, null, null, null, null, groqKey, primary, null, null, null, null, null, null, null);
    }

    @Test
    void mandaTodasAsVagasNumPromptComUmIdPorVaga() {
        var results = List.of(
                new CvMatchResult(true, 90, List.of("m"), null),
                new CvMatchResult(false, 20, List.of("n"), null));
        when(gemini.analyze(anyString(), eq(2), eq("chave"))).thenReturn(results);

        var jobs = List.of(job("Dev Java"), job("Dev Go"));
        assertEquals(results, gateway.analyze(jobs, "meu curriculo", profile("chave", null, null)));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).analyze(prompt.capture(), eq(2), eq("chave"));
        assertTrue(prompt.getValue().contains("<vaga id=\"1\">\nTítulo: Dev Java"));
        assertTrue(prompt.getValue().contains("<vaga id=\"2\">\nTítulo: Dev Go"));
        assertTrue(prompt.getValue().contains("<curriculo>\nmeu curriculo\n</curriculo>"));
    }

    @Test
    void usaAReservaQuandoAPrincipalFalha() {
        when(groq.adaptCurriculum(anyString(), eq("g"))).thenThrow(new ExternalServiceException("fora do ar"));
        when(gemini.adaptCurriculum(anyString(), eq("k"))).thenReturn("cv");

        assertEquals("cv", gateway.adaptCurriculum(job("Dev"), "cv", profile("k", "g", LlmProvider.GROQ)));
        verify(groq).adaptCurriculum(anyString(), eq("g"));
    }

    @Test
    void semReservaOErroDaUnicaChaveSobe() {
        var cota = RateLimitedException.quota("Groq", Duration.ofMinutes(5));
        when(groq.adaptCurriculum(anyString(), eq("g"))).thenThrow(cota);

        var thrown = assertThrows(RateLimitedException.class,
                () -> gateway.adaptCurriculum(job("Dev"), "cv", profile(null, "g", LlmProvider.GEMINI)));

        assertSame(cota, thrown);
    }

    @Test
    void semNenhumaChaveNaoChamaNinguem() {
        var semChave = profile(" ", null, LlmProvider.GROQ);

        assertThrows(BusinessException.class, () -> gateway.analyze(List.of(job("Dev")), "cv", semChave));
        verifyNoInteractions(gemini, groq);
    }
}
