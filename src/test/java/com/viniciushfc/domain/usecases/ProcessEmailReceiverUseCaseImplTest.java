package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.usecases.impl.ProcessEmailReceiverUseCaseImpl;
import com.viniciushfc.domain.vo.*;
import com.viniciushfc.infra.adapter.out.memory.InMemoryScraperActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessEmailReceiverUseCaseImplTest {

    private static final String ALERT_FROM = "jobalerts-noreply@linkedin.com";
    private static final String TO = "fulano@gmail.com";
    private static final String LINK_1 = "https://www.linkedin.com/comm/jobs/view/111/?trk=a&amp;b=1";
    private static final String LINK_2 = "https://www.linkedin.com/comm/jobs/view/222/";

    @Mock
    EmailReceivedRepository emailReceivedRepository;
    @Mock
    JobDetailsGateway jobDetailsGateway;
    @Mock
    AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    @Mock
    PdfGateway pdfGateway;
    @Mock
    JobMatchRepository jobMatchRepository;
    @Spy
    InMemoryScraperActivityRepository scraperActivityRepository = new InMemoryScraperActivityRepository();

    @InjectMocks
    ProcessEmailReceiverUseCaseImpl useCase;

    private Profile user;
    private LinkedAccount linkedin;

    @BeforeEach
    void setUp() {
        linkedin = LinkedAccount.create("login@linkedin", "senha", ExternalProvider.LINKEDIN, null);
        user = userWith("chave-gemini", linkedin);
    }

    private Profile userWith(String geminiKey, LinkedAccount... accounts) {
        return new Profile(UUID.randomUUID(), "Fulano", geminiKey,
                new Attachment("cv.pdf", "application/pdf", new byte[]{1}), new ArrayList<>(List.of(accounts)), null, null);
    }

    private EmailReceived alertEmail(String body) {
        return EmailReceived.create("msg-1", new Email(ALERT_FROM, TO, "Vagas", body, List.of()));
    }

    private JobDetail job(String id) {
        return new JobDetail("https://www.linkedin.com/jobs/view/" + id + "/", "Dev " + id, "desc",
                new CompanyDetails("Empresa " + id, "sobre"));
    }

    private CvMatchResult result(boolean match, int score) {
        return new CvMatchResult(match, score, List.of("motivo"), null);
    }

    @Test
    void ignoraEmailQueNaoEhAlertaDeVaga() throws Exception {
        var email = EmailReceived.create("msg-1", new Email("outro@x.com", TO, "Oi", "corpo", List.of()));

        useCase.execute(user, email);

        verifyNoInteractions(emailReceivedRepository, jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void ignoraEmailJaProcessadoComSucesso() throws Exception {
        var email = alertEmail(LINK_1);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(new EmailReceived(UUID.randomUUID(), "msg-1", email.email(), true));

        useCase.execute(user, email);

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void falhaAntesDoScrapingSemContaDoLinkedin() {
        var semLinkedin = userWith("chave");

        var e = assertThrows(BusinessException.class, () -> useCase.execute(semLinkedin, alertEmail(LINK_1)));
        assertEquals(ErrorCode.LINKEDIN_NOT_CONNECTED, e.code());

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
    }

    @Test
    void falhaAntesDoScrapingSemChaveDoGemini() {
        var semChave = userWith(" ", linkedin);

        var e = assertThrows(BusinessException.class, () -> useCase.execute(semChave, alertEmail(LINK_1)));
        assertEquals(ErrorCode.GEMINI_KEY_MISSING, e.code());

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
    }

    @Test
    void extraiLinksSemDuplicarEUsaContaDoLinkedin() throws Exception {
        var email = alertEmail("<a href=\"" + LINK_1 + "\">x</a> <a href=\"" + LINK_1 + "\">y</a> " + LINK_2);
        when(jobDetailsGateway.extractJobsDetails(any(), any(), anyBoolean())).thenReturn(List.of());

        useCase.execute(user, email);

        verify(jobDetailsGateway).extractJobsDetails(new LinkedHashSet<>(List.of(
                "https://www.linkedin.com/comm/jobs/view/111/?trk=a&b=1",
                LINK_2)), linkedin, false);
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void naoAbreNoLinkedinVagaJaAnalisadaEAnalisaAsNovas() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var nova = job("222");
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);
        when(jobDetailsGateway.extractJobsDetails(Set.of(LINK_2), linkedin, false)).thenReturn(List.of(nova));
        when(pdfGateway.extractTextPdf(user.curriculum())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(List.of(nova), "texto cv", "chave-gemini")).thenReturn(List.of(result(true, 85)));
        when(analyzeJobsMatchCvGateway.adaptCurriculum(nova, "texto cv", "chave-gemini")).thenReturn("cv ats");

        assertTrue(useCase.execute(user, email), "acessou o LinkedIn");

        verify(emailReceivedRepository).save(email);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobMatchRepository).saveAll(captor.capture());
        JobMatch saved = captor.getValue().getFirst();
        assertEquals(1, captor.getValue().size());
        assertEquals(email.id(), saved.emailReceivedId());
        assertEquals(nova.link(), saved.jobLink());
        assertEquals("cv ats", saved.curriculoAts(), "currículo adaptado vem da segunda chamada");
        verify(emailReceivedRepository).markSuccessProcessed(email.id());

        var events = scraperActivityRepository.get().events().stream().map(e -> e.message()).toList();
        assertEquals("Análise concluída: 1 compatível de 1", events.get(0));
        assertEquals("Vaga compatível: Gemini escrevendo o currículo adaptado para Dev 222", events.get(1));
        assertEquals("Gemini analisando 1 vaga de uma vez", events.get(2));
        assertFalse(scraperActivityRepository.get().analyzing());
    }

    @Test
    void alertaSoComVagasJaAnalisadasNaoAbreOLinkedin() throws Exception {
        var email = alertEmail(LINK_1);
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);

        assertFalse(useCase.execute(user, email));

        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void abreONavegadorQuandoOUsuarioPede() throws Exception {
        var visivel = new Profile(user.id(), "F", "chave", user.curriculum(), new ArrayList<>(List.of(linkedin)), true, null);
        when(jobDetailsGateway.extractJobsDetails(any(), any(), anyBoolean())).thenReturn(List.of());

        useCase.execute(visivel, alertEmail(LINK_1));

        verify(jobDetailsGateway).extractJobsDetails(any(), eq(linkedin), eq(true));
    }

    @Test
    void reprocessaEmailQueFalhouReaproveitandoORegistro() throws Exception {
        var email = alertEmail(LINK_1);
        // o registro salvo não tem conteúdo: os links vêm do email recebido agora
        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        when(jobDetailsGateway.extractJobsDetails(any(), any(), anyBoolean())).thenReturn(List.of(job("111")));
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(anyList(), anyString(), anyString())).thenReturn(List.of(result(false, 40)));

        useCase.execute(user, email);

        verify(emailReceivedRepository, never()).save(any());
        verify(emailReceivedRepository).markSuccessProcessed(falhou.id());
    }

}
