package com.viniciushfc.infra.adapter.out.llm;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.vo.ChatMessage;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        return new Profile(null, "F", geminiKey, null, null, null, null, groqKey, primary, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void mandaTodasAsVagasNumPromptComUmIdPorVaga() {
        var results = List.of(
                new CvMatchResult(true, 90, List.of("m"), null),
                new CvMatchResult(false, 20, List.of("n"), null));
        when(gemini.analyze(anyString(), eq(2), eq("chave"), isNull())).thenReturn(results);

        var jobs = List.of(job("Dev Java"), job("Dev Go"));
        assertEquals(results, gateway.analyze(jobs, "meu curriculo", profile("chave", null, null)));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).analyze(prompt.capture(), eq(2), eq("chave"), isNull());
        assertTrue(prompt.getValue().contains("<vaga id=\"1\">\nTítulo: Dev Java"));
        assertTrue(prompt.getValue().contains("<vaga id=\"2\">\nTítulo: Dev Go"));
        assertTrue(prompt.getValue().contains("<curriculo>\nmeu curriculo\n</curriculo>"));
    }

    @Test
    void chatLevaAnaliseEConversaENaoUsaIaForaDaEtapa() {
        var profile = new Profile(null, "F", "k", null, null, null, null, "g", LlmProvider.GEMINI, null, null, null, null, null, null, null, null,
                List.of(LlmProvider.GROQ));
        var jobMatch = JobMatch.create(null, null, job("Dev Java"), new CvMatchResult(false, 55, List.of("falta Kafka"), null));
        when(groq.chat(anyString(), eq("g"), isNull())).thenReturn("resposta");

        var messages = List.of(new ChatMessage("user", "por que 55?"), new ChatMessage("assistant", "falta Kafka"), new ChatMessage("user", "e agora?"));
        assertEquals("resposta", gateway.chat(jobMatch, "meu curriculo", messages, profile));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(groq).chat(prompt.capture(), eq("g"), isNull());
        assertTrue(prompt.getValue().contains("<analise>\nNota: 55\nCompatível: não\nMotivos:\n- falta Kafka\n</analise>"));
        assertTrue(prompt.getValue().contains("Candidato: por que 55?\n\nVocê: falta Kafka\n\nCandidato: e agora?\n\n</conversa>"));
        verifyNoInteractions(gemini);
    }

    @Test
    void curriculoSemUmNumeroDoOriginalPedeDeNovoEFicaComOMelhor() {
        when(gemini.adaptCurriculum(anyString(), eq("k"), isNull()))
                .thenReturn("- Reverti correções do LLM")
                .thenReturn("- Reverti 3 correções inválidas do LLM");

        assertEquals("- Reverti 3 correções inválidas do LLM",
                gateway.adaptCurriculum(job("Dev"), "Reverti 3 correções inválidas do LLM", profile("k", null, null)));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini, times(2)).adaptCurriculum(prompt.capture(), eq("k"), isNull());
        assertTrue(prompt.getAllValues().get(1).contains("deixou de fora estes números do <curriculo>: 3."));
    }

    @Test
    void respostaDoChatSemCurriculoNaoChecaNumeros() {
        var jobMatch = JobMatch.create(null, null, job("Dev"), new CvMatchResult(true, 80, List.of("ok"), null));
        when(gemini.chat(anyString(), eq("k"), isNull())).thenReturn("Falta Kafka.");

        assertEquals("Falta Kafka.", gateway.chat(jobMatch, "Reverti 3 correções", List.of(new ChatMessage("user", "e aí?")), profile("k", null, null)));
        verify(gemini, times(1)).chat(anyString(), eq("k"), isNull());
    }

    @Test
    void usaAReservaQuandoAPrincipalFalha() {
        when(groq.adaptCurriculum(anyString(), eq("g"), isNull())).thenThrow(new ExternalServiceException("fora do ar"));
        when(gemini.adaptCurriculum(anyString(), eq("k"), isNull())).thenReturn("cv");

        assertEquals("cv", gateway.adaptCurriculum(job("Dev"), "cv", profile("k", "g", LlmProvider.GROQ)));
        verify(groq).adaptCurriculum(anyString(), eq("g"), isNull());
    }

    @Test
    void semReservaOErroDaUnicaChaveSobe() {
        var cota = RateLimitedException.quota("Groq", Duration.ofMinutes(5));
        when(groq.adaptCurriculum(anyString(), eq("g"), isNull())).thenThrow(cota);

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
