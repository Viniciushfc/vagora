package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.usecases.impl.ProcessEmailReceiverUseCaseImpl;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    JobMatchRepository jobMatchRepository;
    @Mock
    PendingJobRepository pendingJobRepository;

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
                new Attachment("cv.pdf", "application/pdf", new byte[]{1}), new ArrayList<>(List.of(accounts)), null, null, null, null, null, null, null, null, null, null, null, null);
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
        return EmailReceived.create(messageId, new Email("Indeed <indeedapply@indeed.com>", TO, "Vaga", body, List.of()));
    }

    private List<PendingJob> savedPending() {
        ArgumentCaptor<PendingJob> captor = ArgumentCaptor.forClass(PendingJob.class);
        verify(pendingJobRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void ignoraEmailQueNaoEhAlertaDeVaga() throws Exception {
        var email = EmailReceived.create("msg-1", new Email("outro@x.com", TO, "Oi", "corpo", List.of()));

        useCase.execute(user, LINKEDIN, List.of(email));

        verifyNoInteractions(emailReceivedRepository, jobDetailsGateway, jobMatchRepository, pendingJobRepository);
    }

    @Test
    void ignoraEmailJaProcessadoComSucesso() throws Exception {
        var email = alertEmail(LINK_1);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(new EmailReceived(UUID.randomUUID(), "msg-1", email.email(), true));

        useCase.execute(user, LINKEDIN, List.of(email));

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, jobMatchRepository, pendingJobRepository);
    }

    @Test
    void falhaAntesDoScrapingSemContaDoLinkedin() {
        var semLinkedin = userWith("chave");

        var e = assertThrows(BusinessException.class, () -> useCase.execute(semLinkedin, LINKEDIN, List.of(alertEmail(LINK_1))));
        assertEquals(ErrorCode.LINKEDIN_NOT_CONNECTED, e.code());

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway);
    }

    @Test
    void semChaveDoGeminiLeAsVagasMesmoAssim() throws Exception {
        var semChave = userWith(" ", linkedin);
        scrapes(Set.of(LINK_2), job("222"));

        assertTrue(useCase.execute(semChave, LINKEDIN, List.of(alertEmail(LINK_2))));

        assertEquals(1, savedPending().size(), "a análise espera a chave na fila");
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
    void guardaAsVagasLidasComoPendentesSemChamarOGemini() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var nova = job("222");
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);
        scrapes(Set.of(LINK_2), nova);

        assertTrue(useCase.execute(user, LINKEDIN, List.of(email)), "acessou o LinkedIn");

        verify(emailReceivedRepository).save(email);
        var pendentes = savedPending();
        assertEquals(1, pendentes.size());
        PendingJob pendente = pendentes.getFirst();
        assertEquals(nova, pendente.job());
        assertEquals(user.id(), pendente.profileId());
        assertEquals(email.id(), pendente.emailReceivedId());
        assertEquals(LINKEDIN, pendente.source());
        assertFalse(pendente.prioritized());
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void vagaJaNaFilaNaoAbreDeNovo() throws Exception {
        var email = alertEmail(LINK_2);
        when(pendingJobRepository.existsByProfileIdAndLink(user.id(), "https://www.linkedin.com/jobs/view/222/")).thenReturn(true);

        assertFalse(useCase.execute(user, LINKEDIN, List.of(email)));

        verifyNoInteractions(jobDetailsGateway);
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void alertaSoComVagasJaAnalisadasNaoAbreOLinkedin() throws Exception {
        var email = alertEmail(LINK_1);
        when(jobMatchRepository.existsByProfileIdAndJobLink(user.id(), "https://www.linkedin.com/jobs/view/111/")).thenReturn(true);

        assertFalse(useCase.execute(user, LINKEDIN, List.of(email)));

        verifyNoInteractions(jobDetailsGateway);
        verify(pendingJobRepository, never()).save(any());
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void abreONavegadorQuandoOUsuarioPede() throws Exception {
        var visivel = new Profile(user.id(), "F", "chave", user.curriculum(), new ArrayList<>(List.of(linkedin)), true, null, null, null, null, null, null, null, null, null, null, null);

        useCase.execute(visivel, LINKEDIN, List.of(alertEmail(LINK_1)));

        verify(jobDetailsGateway).extractJobsDetails(any(), any(), eq(linkedin), eq(true), any());
    }

    @Test
    void reprocessaEmailQueFalhouReaproveitandoORegistro() throws Exception {
        var email = alertEmail(LINK_1);

        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        scrapes(Set.of("https://www.linkedin.com/comm/jobs/view/111/?trk=a&b=1"), job("111"));

        useCase.execute(user, LINKEDIN, List.of(email));

        verify(emailReceivedRepository, never()).save(any());
        assertEquals(falhou.id(), savedPending().getFirst().emailReceivedId());
        verify(emailReceivedRepository).markSuccessProcessed(falhou.id());
    }

    @Test
    void vagasGuardadasPorVersaoAntigaEntramNaFilaSemAbrirOLinkedin() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var falhou = new EmailReceived(UUID.randomUUID(), "msg-1", null, false);
        var guardada = job("111");
        var nova = job("222");
        when(emailReceivedRepository.findByMessageId("msg-1")).thenReturn(falhou);
        when(emailReceivedRepository.findScrapedJobs(falhou.id())).thenReturn(List.of(guardada));
        scrapes(Set.of(LINK_2), nova);

        assertTrue(useCase.execute(user, LINKEDIN, List.of(email)));

        assertEquals(List.of(guardada, nova), savedPending().stream().map(PendingJob::job).toList());
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

        assertEquals(List.of(lida), savedPending().stream().map(PendingJob::job).toList());
        verify(emailReceivedRepository, never()).markSuccessProcessed(any());
    }

    @Test
    void indeedLeOsEmailsDaFilaJuntosSemContaDoLinkedin() throws Exception {
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

        assertTrue(useCase.execute(semLinkedin, AlertSource.INDEED, List.of(email1, email2)));

        var salvas = savedPending();
        assertEquals("https://br.indeed.com/viewjob?jk=aa11", salvas.get(0).link());
        assertEquals(email1.id(), salvas.get(0).emailReceivedId());
        assertEquals("https://br.indeed.com/viewjob?jk=bb22", salvas.get(1).link());
        assertEquals(email2.id(), salvas.get(1).emailReceivedId());
        assertEquals(AlertSource.INDEED, salvas.get(1).source());
        verify(emailReceivedRepository).markSuccessProcessed(email1.id());
        verify(emailReceivedRepository).markSuccessProcessed(email2.id());
    }
}
