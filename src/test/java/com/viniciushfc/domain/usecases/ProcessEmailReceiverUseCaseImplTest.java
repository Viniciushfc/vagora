package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.impl.ProcessEmailReceiverUseCaseImpl;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessEmailReceiverUseCaseImplTest {

    private static final String ALERT_FROM = "LinkedIn <jobalerts-noreply@linkedin.com>";
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
    UserAccountRepository userAccountRepository;
    @Mock
    PdfGateway pdfGateway;
    @Mock
    JobMatchRepository jobMatchRepository;

    @InjectMocks
    ProcessEmailReceiverUseCaseImpl useCase;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = new UserAccount(UUID.randomUUID(), "Fulano", "fulano", "123",
                new Attachment("cv.pdf", "application/pdf", new byte[]{1}), new ArrayList<>());
    }

    private EmailReceived alertEmail(String body) {
        return EmailReceived.create("msg-1", new Email(ALERT_FROM, TO, "Vagas", body, List.of()));
    }

    private JobDetail job(String id) {
        return new JobDetail("https://www.linkedin.com/jobs/view/" + id + "/", "Dev " + id, "desc",
                new CompanyDetails("Empresa " + id, "sobre"));
    }

    private CvMatchResult result(boolean match, int score) {
        return new CvMatchResult(match, score, List.of("motivo"), match ? "cv ats" : null);
    }

    @Test
    void ignoraEmailQueNaoEhAlertaDeVaga() throws Exception {
        var email = EmailReceived.create("msg-1", new Email("outro@x.com", TO, "Oi", "corpo", List.of()));

        useCase.execute(email);

        verifyNoInteractions(emailReceivedRepository, jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void ignoraEmailJaRecebido() throws Exception {
        var email = alertEmail(LINK_1);
        when(emailReceivedRepository.existsEmailByMessageId("msg-1")).thenReturn(true);

        useCase.execute(email);

        verify(emailReceivedRepository, never()).save(any());
        verifyNoInteractions(jobDetailsGateway, analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void extraiLinksSemDuplicarEDecodificaAmp() throws Exception {
        var email = alertEmail("<a href=\"" + LINK_1 + "\">x</a> <a href=\"" + LINK_1 + "\">y</a> " + LINK_2);
        when(userAccountRepository.findByLinkedUsername(TO)).thenReturn(user);
        when(jobDetailsGateway.extractJobsDetails(any())).thenReturn(List.of());

        useCase.execute(email);

        verify(jobDetailsGateway).extractJobsDetails(new LinkedHashSet<>(List.of(
                "https://www.linkedin.com/comm/jobs/view/111/?trk=a&b=1",
                LINK_2)));
    }

    @Test
    void lancaErroQuandoNaoHaUsuarioVinculado() {
        var email = alertEmail(LINK_1);
        when(jobDetailsGateway.extractJobsDetails(any())).thenReturn(List.of(job("111")));

        assertThrows(IllegalStateException.class, () -> useCase.execute(email));

        verify(emailReceivedRepository, never()).markSuccessProcessed(any());
        verifyNoInteractions(analyzeJobsMatchCvGateway, jobMatchRepository);
    }

    @Test
    void naoAnalisaQuandoTodasAsVagasJaForamAnalisadas() throws Exception {
        var email = alertEmail(LINK_1);
        var job = job("111");
        when(jobDetailsGateway.extractJobsDetails(any())).thenReturn(List.of(job));
        when(userAccountRepository.findByLinkedUsername(TO)).thenReturn(user);
        when(jobMatchRepository.existsByUserAccountIdAndJobLink(user.id(), job.link())).thenReturn(true);

        useCase.execute(email);

        verifyNoInteractions(analyzeJobsMatchCvGateway, pdfGateway);
        verify(jobMatchRepository, never()).saveAll(any());
        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void analisaSomenteVagasNovasESalvaOsMatches() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        var jaAnalisada = job("111");
        var nova = job("222");
        when(jobDetailsGateway.extractJobsDetails(any())).thenReturn(List.of(jaAnalisada, nova));
        when(userAccountRepository.findByLinkedUsername(TO)).thenReturn(user);
        when(jobMatchRepository.existsByUserAccountIdAndJobLink(user.id(), jaAnalisada.link())).thenReturn(true);
        when(jobMatchRepository.existsByUserAccountIdAndJobLink(user.id(), nova.link())).thenReturn(false);
        when(pdfGateway.extractTextPdf(user.curriculum())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.executeAnalysis(List.of(nova), "texto cv")).thenReturn(List.of(result(true, 85)));

        useCase.execute(email);

        verify(emailReceivedRepository).save(email);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobMatchRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());

        JobMatch saved = captor.getValue().getFirst();
        assertEquals(user.id(), saved.userAccountId());
        assertEquals(email.id(), saved.emailReceivedId());
        assertEquals(nova.link(), saved.jobLink());
        assertEquals("Dev 222", saved.jobTitle());
        assertEquals("Empresa 222", saved.companyName());
        assertTrue(saved.match());
        assertEquals(85, saved.score());

        verify(emailReceivedRepository).markSuccessProcessed(email.id());
    }

    @Test
    void lancaErroQuandoQuantidadeDeResultadosDifereDasVagas() throws Exception {
        var email = alertEmail(LINK_1 + " " + LINK_2);
        when(jobDetailsGateway.extractJobsDetails(any())).thenReturn(List.of(job("111"), job("222")));
        when(userAccountRepository.findByLinkedUsername(TO)).thenReturn(user);
        when(pdfGateway.extractTextPdf(any())).thenReturn("texto cv");
        when(analyzeJobsMatchCvGateway.executeAnalysis(anyList(), anyString())).thenReturn(List.of(result(false, 10)));

        assertThrows(IllegalStateException.class, () -> useCase.execute(email));

        verify(jobMatchRepository, never()).saveAll(any());
        verify(emailReceivedRepository, never()).markSuccessProcessed(any());
    }
}
