package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.impl.AnalyzePendingJobsUseCaseImpl;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.memory.InMemoryProcessingStatusRepository;
import com.viniciushfc.infra.adapter.out.memory.InMemoryScraperActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.viniciushfc.domain.enums.AlertSource.INDEED;
import static com.viniciushfc.domain.enums.AlertSource.LINKEDIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzePendingJobsUseCaseImplTest {

    @Mock
    ProfileRepository profileRepository;
    @Mock
    PendingJobRepository pendingJobRepository;
    @Mock
    JobMatchRepository jobMatchRepository;
    @Mock
    AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    @Mock
    PdfGateway pdfGateway;

    private InMemoryProcessingStatusRepository statusRepository;
    private InMemoryScraperActivityRepository activityRepository;
    private AnalyzePendingJobsUseCaseImpl useCase;
    private Profile user;

    @BeforeEach
    void setUp() {
        statusRepository = new InMemoryProcessingStatusRepository();
        activityRepository = new InMemoryScraperActivityRepository();
        useCase = new AnalyzePendingJobsUseCaseImpl(profileRepository, pendingJobRepository, jobMatchRepository,
                analyzeJobsMatchCvGateway, pdfGateway, statusRepository, activityRepository);
        user = profile("chave", null);
        lenient().when(profileRepository.find()).thenReturn(user);
        lenient().when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
    }

    private Profile profile(String key, Boolean monitoring) {
        return new Profile(UUID.randomUUID(), "F", key, new Attachment("cv.pdf", "application/pdf", new byte[]{1}),
                new ArrayList<>(), null, monitoring, null, null);
    }

    private PendingJob pending(String id, AlertSource source, int attempts) {
        var job = new JobDetail("https://www.linkedin.com/jobs/view/" + id + "/", "Dev " + id, "desc", new CompanyDetails("ACME", null));
        return new PendingJob(UUID.randomUUID(), user.id(), UUID.randomUUID(), source, job, false, attempts, LocalDateTime.now());
    }

    private PendingJob pendingWithDescription(String id, String description) {
        var job = new JobDetail("https://www.linkedin.com/jobs/view/" + id + "/", "Dev " + id, description, null);
        return new PendingJob(UUID.randomUUID(), user.id(), UUID.randomUUID(), LINKEDIN, job, false, 0, LocalDateTime.now());
    }

    private void queue(PendingJob... jobs) {
        when(pendingJobRepository.nextBatch(user.id(), AnalyzePendingJobsUseCaseImpl.MAX_ATTEMPTS, AnalyzePendingJobsUseCaseImpl.BATCH_SIZE))
                .thenReturn(List.of(jobs));
    }

    private static CvMatchResult result(boolean match, int score) {
        return new CvMatchResult(match, score, List.of("motivo"), null);
    }

    @Test
    void analisaOLoteSalvaOResultadoETiraDaFila() {
        var compativel = pending("1", LINKEDIN, 0);
        var fraca = pending("2", LINKEDIN, 0);
        queue(compativel, fraca);
        when(analyzeJobsMatchCvGateway.analyze(List.of(compativel.job(), fraca.job()), "texto cv", user))
                .thenReturn(List.of(result(true, 85), result(false, 30)));
        when(analyzeJobsMatchCvGateway.adaptCurriculum(compativel.job(), "texto cv", user)).thenReturn("cv ats");

        useCase.execute();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobMatchRepository).saveAll(captor.capture());
        var salvas = captor.getValue();
        assertEquals(compativel.emailReceivedId(), salvas.getFirst().emailReceivedId());
        assertEquals(compativel.link(), salvas.getFirst().jobLink());
        verify(pendingJobRepository).delete(List.of(compativel.id(), fraca.id()));
        verify(jobMatchRepository).updateCurriculum(salvas.getFirst().id(), "cv ats");
        verify(analyzeJobsMatchCvGateway, never()).adaptCurriculum(eq(fraca.job()), any(), any());
        assertEquals("Análise concluída: 1 compatível de 2", activityRepository.get(LINKEDIN).events().getFirst().message());
        assertNull(statusRepository.findAnalysis(user.id()).lastError());
    }

    @Test
    void semChaveOuComMonitoramentoPausadoNaoAnalisa() {
        when(profileRepository.find()).thenReturn(profile(" ", null), profile("chave", false));

        useCase.execute();
        useCase.execute();

        verifyNoInteractions(pendingJobRepository, analyzeJobsMatchCvGateway);
    }

    @Test
    void esperaOLeitorDaFonteTerminar() {
        queue(pending("1", LINKEDIN, 0));
        activityRepository.save(LINKEDIN, activityRepository.get(LINKEDIN).started(5));

        useCase.execute();

        verifyNoInteractions(analyzeJobsMatchCvGateway);
        assertTrue(activityRepository.get(LINKEDIN).running());
    }

    @Test
    void loteTemVagasDeUmaFonteSo() {
        var linkedin = pending("1", LINKEDIN, 0);
        var indeed = pending("2", INDEED, 0);
        queue(linkedin, indeed);
        when(analyzeJobsMatchCvGateway.analyze(anyList(), any(), any())).thenReturn(List.of(result(false, 10)));

        useCase.execute();

        verify(analyzeJobsMatchCvGateway).analyze(eq(List.of(linkedin.job())), any(), any());
        assertTrue(activityRepository.get(INDEED).events().isEmpty(), "a vaga do Indeed fica para o próximo lote");
    }

    @Test
    void comChaveDoGroqOLoteCabeNoLimiteDeTexto() {
        user = new Profile(user.id(), "F", "chave", user.curriculum(), new ArrayList<>(),
                null, null, "gsk", LlmProvider.GROQ);
        when(profileRepository.find()).thenReturn(user);
        String metadeDoLimite = "x".repeat(LlmProvider.GROQ.maxJobChars / 2);
        var primeira = pendingWithDescription("1", metadeDoLimite);
        var segunda = pendingWithDescription("2", metadeDoLimite);
        var terceira = pendingWithDescription("3", metadeDoLimite);
        queue(primeira, segunda, terceira);
        when(analyzeJobsMatchCvGateway.analyze(anyList(), any(), any()))
                .thenReturn(List.of(result(false, 10), result(false, 10)));

        useCase.execute();

        verify(analyzeJobsMatchCvGateway).analyze(eq(List.of(primeira.job(), segunda.job())), any(), any());
    }

    @Test
    void respostaInvalidaContaFalhaEAVagaQueJaFalhouVaiSozinha() {
        var a = pending("1", LINKEDIN, 0);
        var b = pending("2", LINKEDIN, 0);
        queue(a, b);
        when(analyzeJobsMatchCvGateway.analyze(anyList(), any(), any()))
                .thenThrow(new ExternalServiceException("fora do formato", ErrorCode.LLM_INVALID_RESPONSE));

        useCase.execute();

        verify(pendingJobRepository).registerFailure(List.of(a.id(), b.id()));
        verify(jobMatchRepository, never()).saveAll(any());
        var status = statusRepository.findAnalysis(user.id());
        assertEquals("fora do formato", status.lastError());
        assertFalse(status.isWaiting(LocalDateTime.now()), "a próxima tentativa separa a vaga problemática sem esperar");

        var jaFalhou = pending("1", LINKEDIN, 1);
        queue(jaFalhou, pending("2", LINKEDIN, 1));
        useCase.execute();
        verify(analyzeJobsMatchCvGateway).analyze(eq(List.of(jaFalhou.job())), any(), any());
    }

    @Test
    void cotaDoGeminiEsperaOTempoPedidoSemContarFalha() {
        queue(pending("1", LINKEDIN, 0));
        when(analyzeJobsMatchCvGateway.analyze(anyList(), any(), any()))
                .thenThrow(new RateLimitedException("cota", ErrorCode.LLM_QUOTA_EXCEEDED, Duration.ofHours(2)));

        useCase.execute();

        verify(pendingJobRepository, never()).registerFailure(any());
        var status = statusRepository.findAnalysis(user.id());
        assertEquals(ErrorCode.LLM_QUOTA_EXCEEDED, status.lastErrorCode());
        assertTrue(status.nextAlertAt().isAfter(LocalDateTime.now().plusMinutes(110)));

        useCase.execute();
        verify(analyzeJobsMatchCvGateway, times(1)).analyze(anyList(), any(), any());
    }

    @Test
    void vagaJaAnalisadaSoSaiDaFila() {
        var repetida = pending("1", LINKEDIN, 0);
        queue(repetida);
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), repetida.link())).thenReturn(true);

        useCase.execute();

        verify(pendingJobRepository).delete(List.of(repetida.id()));
        verifyNoInteractions(analyzeJobsMatchCvGateway);
    }

    @Test
    void falhaNoCurriculoNaoDesfazAAnalise() {
        var vaga = pending("1", LINKEDIN, 0);
        queue(vaga);
        when(analyzeJobsMatchCvGateway.analyze(anyList(), any(), any())).thenReturn(List.of(result(true, 90)));
        when(analyzeJobsMatchCvGateway.adaptCurriculum(any(), any(), any()))
                .thenThrow(new ExternalServiceException("O Gemini demorou demais para responder."));

        useCase.execute();

        verify(jobMatchRepository).saveAll(argThat(saved -> saved.size() == 1 && saved.getFirst().match()));
        verify(pendingJobRepository).delete(List.of(vaga.id()));
        verify(jobMatchRepository, never()).updateCurriculum(any(), any());
        assertNull(statusRepository.findAnalysis(user.id()).lastError());
    }
}
