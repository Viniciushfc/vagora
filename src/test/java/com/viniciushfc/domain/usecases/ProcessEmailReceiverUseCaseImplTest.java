package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.exceptions.ExternalServiceException;
import static org.mockito.ArgumentMatchers.argThat;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import static com.viniciushfc.domain.enums.AlertSource.LINKEDIN;
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

    private JobDetail readFrom(Set<String> links, JobDetail job) {
        String id = job.link().replaceAll("\\D+", "");
        String link = links.stream().filter(l -> l.contains("/view/" + id)).findFirst().orElseThrow();
        return new JobDetail(link, job.title(), job.description(), job.companyDetails());
    }

    @SuppressWarnings("unchecked")
    private void scrapes(Set<String> links, JobDetail... jobs) {
        doAnswer(inv -> {
            List.of(jobs).forEach(job -> inv.<Consumer<JobDetail>>getArgument(4).accept(readFrom(links, job)));
            return null;
        }).when(jobDetailsGateway).extractJobsDetails(eq(LINKEDIN), eq(links), any(), anyBoolean(), any(Consumer.class));
    }

    private static String indeedLink(String target) throws Exception {
        var zipped = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(zipped)) {
            gzip.write(("{\"u\":\"" + target + "\",\"m\":{}}").getBytes(StandardCharsets.UTF_8));
        }
        return "https://cts.indeed.com/v3/" + Base64.getUrlEncoder().withoutPadding().encodeToString(zipped.toByteArray()) + "/assinatura-1";
    }

    private EmailReceived indeedEmail(String messageId, String body) {
        return EmailReceived.create(messageId, new Email("Indeed <donotreply@match.indeed.com>", TO, "Vaga", body, List.of()));
    }

    private CvMatchResult result(boolean match, int score) {
        return new CvMatchResult(match, score, List.of("motivo"), null);
    }

    @Test
    void ignoraEmailQueNaoEhAlertaDeVaga() throws Exception {
        var email = EmailReceived.create("msg-1", new Email("outro@x.com", TO, "Oi", "corpo", List.of()));

        useCase.execute(user, LINKEDIN, List.of(email));

        verifyNoInteractions(emailReceivedRepository, jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void ignoraEmailJaProcessadoComSucesso() throws Exception {
        var email = alertEmail(LINK_1);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(new EmailReceived(UUID.randomUUID(), "msg-1", email.email(), true));

        useCase.execute(user, LINKEDIN, List.of(email));

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void falhaAntesDoScrapingSemContaDoLinkedin() {
        var semLinkedin = userWith("chave");

        var e = assertThrows(BusinessException.class, () -> useCase.execute(semLinkedin, LINKEDIN, List.of(alertEmail(LINK_1))));
        assertEquals(ErrorCode.LINKEDIN_NOT_CONNECTED, e.code());

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
    }

    @Test
    void falhaAntesDoScrapingSemChaveDoGemini() {
        var semChave = userWith(" ", linkedin);

        var e = assertThrows(BusinessException.class, () -> useCase.execute(semChave, LINKEDIN, List.of(alertEmail(LINK_1))));
        assertEquals(ErrorCode.GEMINI_KEY_MISSING, e.code());

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
    }

    @Test
    void extraiLinksSemDuplicarEUsaContaDoLinkedin() throws Exception {
        var email = alertEmail("<a href=\"" + LINK_1 + "\">x</a> <a href=\"" + LINK_1 + "\">y</a> " + LINK_2);

        useCase.execute(user, LINKEDIN, List.of(email));

        verify(jobDetailsGateway).extractJobsDetails(eq(LINKEDIN), eq(new LinkedHashSet<>(List.of(
                "https://www.linkedin.com/comm/jobs/view/111/?trk=a&b=1",
                LINK_2))), eq(linkedin), eq(false), any());
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void naoAbreNoLinkedinVagaJaAnalisadaEAnalisaAsNovas() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var nova = job("222");
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);
        scrapes(Set.of(LINK_2), nova);
        when(pdfGateway.extractTextPdf(user.curriculum())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(List.of(nova), "texto cv", "chave-gemini")).thenReturn(List.of(result(true, 85)));
        when(analyzeJobsMatchCvGateway.adaptCurriculum(any(), eq("texto cv"), eq("chave-gemini"))).thenReturn("cv ats");

        assertTrue(useCase.execute(user, LINKEDIN, List.of(email)), "acessou o LinkedIn");

        verify(emailReceivedRepository).save(email);
        verify(emailReceivedRepository).addScrapedJob(email.id(), nova);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobMatchRepository).saveAll(captor.capture());
        JobMatch saved = captor.getValue().getFirst();
        assertEquals(1, captor.getValue().size());
        assertEquals(email.id(), saved.emailReceivedId());
        assertEquals(nova.link(), saved.jobLink());
        verify(jobMatchRepository).updateCurriculum(saved.id(), "cv ats");
        verify(emailReceivedRepository).markSuccessProcessed(email.id());

        var events = scraperActivityRepository.get(LINKEDIN).events().stream().map(e -> e.message()).toList();
        assertEquals("Análise concluída: 1 compatível de 1", events.get(0));
        assertEquals("Vaga compatível: Gemini escrevendo o currículo adaptado para Dev 222", events.get(1));
        assertEquals("Gemini analisando 1 vaga de uma vez", events.get(2));
        assertFalse(scraperActivityRepository.get(LINKEDIN).analyzing());
    }

    @Test
    void alertaSoComVagasJaAnalisadasNaoAbreOLinkedin() throws Exception {
        var email = alertEmail(LINK_1);
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);

        assertFalse(useCase.execute(user, LINKEDIN, List.of(email)));

        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway);
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void abreONavegadorQuandoOUsuarioPede() throws Exception {
        var visivel = new Profile(user.id(), "F", "chave", user.curriculum(), new ArrayList<>(List.of(linkedin)), true, null);

        useCase.execute(visivel, LINKEDIN, List.of(alertEmail(LINK_1)));

        verify(jobDetailsGateway).extractJobsDetails(any(), any(), eq(linkedin), eq(true), any());
    }

    @Test
    void reprocessaEmailQueFalhouReaproveitandoORegistro() throws Exception {
        var email = alertEmail(LINK_1);
        // o registro salvo não tem conteúdo: os links vêm do email recebido agora
        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        scrapes(Set.of("https://www.linkedin.com/comm/jobs/view/111/?trk=a&b=1"), job("111"));
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(anyList(), anyString(), anyString())).thenReturn(List.of(result(false, 40)));

        useCase.execute(user, LINKEDIN, List.of(email));

        verify(emailReceivedRepository, never()).save(any());
        verify(emailReceivedRepository).markSuccessProcessed(falhou.id());
    }

    @Test
    void vagaLidaNumaTentativaAnteriorNaoAbreDeNovoNoLinkedin() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        var guardada = job("111");
        var nova = job("222");
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        when(emailReceivedRepository.findScrapedJobs(falhou.id())).thenReturn(List.of(guardada));
        scrapes(Set.of(LINK_2), nova);
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(List.of(guardada, nova), "texto cv", "chave-gemini"))
                .thenReturn(List.of(result(false, 40), result(false, 30)));

        assertTrue(useCase.execute(user, LINKEDIN, List.of(email)));

        verify(emailReceivedRepository).addScrapedJob(falhou.id(), nova);
        verify(emailReceivedRepository, never()).addScrapedJob(falhou.id(), guardada);
        verify(jobMatchRepository).saveAll(argThat(saved -> saved.size() == 2));
        verify(emailReceivedRepository).markSuccessProcessed(falhou.id());
    }

    @Test
    void soVagasGuardadasVaoDiretoAoGeminiSemAbrirOLinkedin() throws Exception {
        var email = alertEmail(LINK_1);
        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        var guardada = job("111");
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        when(emailReceivedRepository.findScrapedJobs(falhou.id())).thenReturn(List.of(guardada));
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(List.of(guardada), "texto cv", "chave-gemini")).thenReturn(List.of(result(false, 40)));

        assertFalse(useCase.execute(user, LINKEDIN, List.of(email)), "não acessou o LinkedIn");

        verifyNoInteractions(jobDetailsGateway);
        verify(emailReceivedRepository).markSuccessProcessed(falhou.id());
    }

    @Test
    void falhaNoMeioDaLeituraGuardaAsVagasJaLidas() {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var lida = job("111");
        doAnswer(inv -> {
            inv.<Consumer<JobDetail>>getArgument(4).accept(readFrom(inv.getArgument(1), lida));
            throw new RuntimeException("navegador fechado");
        }).when(jobDetailsGateway).extractJobsDetails(any(), any(), any(), anyBoolean(), any());

        assertThrows(RuntimeException.class, () -> useCase.execute(user, LINKEDIN, List.of(email)));

        verify(emailReceivedRepository).addScrapedJob(email.id(), lida);
        verify(emailReceivedRepository, never()).markSuccessProcessed(any());
        verifyNoInteractions(analyzeJobsMatchCvGateway);
    }


    @Test
    void falhaNoCurriculoGuardaAAnaliseENaoDerrubaOAlerta() throws Exception {
        var email = alertEmail(LINK_2);
        var vaga = job("222");
        scrapes(Set.of(LINK_2), vaga);
        when(pdfGateway.extractTextPdf(user.curriculum())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(List.of(vaga), "texto cv", "chave-gemini")).thenReturn(List.of(result(true, 90)));
        when(analyzeJobsMatchCvGateway.adaptCurriculum(any(), any(), any()))
                .thenThrow(new ExternalServiceException("O Gemini demorou demais para responder."));

        assertTrue(useCase.execute(user, LINKEDIN, List.of(email)));

        verify(jobMatchRepository).saveAll(argThat(saved -> saved.size() == 1 && saved.getFirst().match()
                && saved.getFirst().jobDescription() != null));
        verify(jobMatchRepository, never()).updateCurriculum(any(), any());
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void indeedLeOsEmailsDaFilaJuntosNumaChamadaSoAoGeminiSemContaDoLinkedin() throws Exception {
        var semLinkedin = userWith("chave-gemini");
        String vaga1 = indeedLink("https://br.indeed.com/rc/clk?jk=aa11&from=email");
        String naoCombina = indeedLink("https://br.indeed.com/feedback?jk=aa11&tipo=ruim");
        String vaga2 = indeedLink("https://br.indeed.com/rc/clk?jk=bb22&from=email");
        var email1 = indeedEmail("i-1", "<a href=\"" + vaga1 + "\">Ver vaga</a> <a href=\"" + naoCombina + "\">Não</a> " + vaga1);
        var email2 = indeedEmail("i-2", "Ver vaga: " + vaga2);
        var lida1 = new JobDetail(vaga1, "Dev 1", "desc", new CompanyDetails("A", null));
        var lida2 = new JobDetail(vaga2, "Dev 2", "desc", new CompanyDetails("B", null));
        doAnswer(inv -> {
            Consumer<JobDetail> onJobRead = inv.getArgument(4);
            onJobRead.accept(lida1);
            onJobRead.accept(lida2);
            return null;
        }).when(jobDetailsGateway).extractJobsDetails(eq(AlertSource.INDEED), eq(new LinkedHashSet<>(List.of(vaga1, vaga2))), isNull(), eq(false), any());
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.analyze(anyList(), eq("texto cv"), eq("chave-gemini")))
                .thenReturn(List.of(result(false, 40), result(false, 30)));

        assertTrue(useCase.execute(semLinkedin, AlertSource.INDEED, List.of(email1, email2)));

        verify(analyzeJobsMatchCvGateway, times(1)).analyze(anyList(), anyString(), anyString());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobMatchRepository).saveAll(captor.capture());
        var salvas = captor.getValue();
        assertEquals("https://br.indeed.com/viewjob?jk=aa11", salvas.get(0).jobLink());
        assertEquals(email1.id(), salvas.get(0).emailReceivedId());
        assertEquals("https://br.indeed.com/viewjob?jk=bb22", salvas.get(1).jobLink());
        assertEquals(email2.id(), salvas.get(1).emailReceivedId());
        verify(emailReceivedRepository).markSuccessProcessed(email1.id());
        verify(emailReceivedRepository).markSuccessProcessed(email2.id());
        assertEquals("Análise concluída: 0 compatíveis de 2", scraperActivityRepository.get(AlertSource.INDEED).events().getFirst().message());
        assertTrue(scraperActivityRepository.get(LINKEDIN).events().isEmpty(), "a análise do Indeed não aparece no leitor do LinkedIn");
    }
}
