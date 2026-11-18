package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.usecases.impl.PollJobAlertsUseCaseImpl;
import com.viniciushfc.domain.vo.Email;
import com.viniciushfc.infra.adapter.out.memory.InMemoryProcessingStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.viniciushfc.domain.enums.AlertSource.INDEED;
import static com.viniciushfc.domain.enums.AlertSource.LINKEDIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollJobAlertsUseCaseImplTest {

    @Mock
    MailboxGateway mailboxGateway;
    @Mock
    ProcessEmailReceiverUseCase processEmailReceiverUseCase;
    @Mock
    ProfileRepository profileRepository;

    private InMemoryProcessingStatusRepository statusRepository;
    private PollJobAlertsUseCaseImpl useCase;

    private final LinkedAccount emailAccount = LinkedAccount.create("f@gmail.com", "app", ExternalProvider.EMAIL, "INBOX");

    @BeforeEach
    void setUp() {
        statusRepository = new InMemoryProcessingStatusRepository();
        useCase = new PollJobAlertsUseCaseImpl(mailboxGateway, processEmailReceiverUseCase, statusRepository, profileRepository);
    }

    private Profile user(LinkedAccount... accounts) {
        Profile profile = new Profile(UUID.randomUUID(), "F", "k", null, new ArrayList<>(List.of(accounts)), null, null, null, null);
        lenient().when(profileRepository.find()).thenReturn(profile);
        return profile;
    }

    private EmailReceived alert(String messageId) {
        return EmailReceived.create(messageId, new Email("jobalerts-noreply@linkedin.com", "f@gmail.com", "Alerta " + messageId, "b", List.of()));
    }

    private void endInterval(Profile user) {
        statusRepository.save(user.id(), LINKEDIN, statusRepository.find(user.id(), LINKEDIN).waitUntil(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void monitoramentoPausadoNaoAbreOEmail() {
        var pausado = new Profile(UUID.randomUUID(), "F", "k", null, new ArrayList<>(List.of(emailAccount)), null, false, null, null);
        when(profileRepository.find()).thenReturn(pausado);

        useCase.execute(LINKEDIN);

        verifyNoInteractions(mailboxGateway, processEmailReceiverUseCase);
    }

    @Test
    void depoisDeAcessarOLinkedinEsperaPeloMenos15Minutos() throws Exception {
        var user = user(emailAccount);
        var primeiro = alert("1");
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(primeiro, alert("2"), alert("3")));
        when(processEmailReceiverUseCase.execute(user, LINKEDIN, List.of(primeiro))).thenReturn(true);

        var antes = LocalDateTime.now();
        useCase.execute(LINKEDIN);

        verify(processEmailReceiverUseCase, times(1)).execute(any(), any(), any());
        verify(processEmailReceiverUseCase).execute(user, LINKEDIN, List.of(primeiro));
        verify(mailboxGateway).markAsRead(emailAccount, LINKEDIN, List.of("1"));

        var status = statusRepository.find(user.id(), LINKEDIN);
        assertEquals(2, status.queuedAlerts());
        assertNull(status.currentAlert());
        assertFalse(status.nextAlertAt().isBefore(antes.plus(PollJobAlertsUseCaseImpl.MIN_INTERVAL)));
        assertFalse(status.nextAlertAt().isAfter(LocalDateTime.now().plus(PollJobAlertsUseCaseImpl.MIN_INTERVAL).plus(PollJobAlertsUseCaseImpl.MAX_JITTER)));
    }

    @Test
    void alertasSemVagaNovaSeResolvemNoMesmoCicloSemIntervalo() throws Exception {
        var user = user(emailAccount);
        var semNovidade = alert("1");
        var comVagaNova = alert("2");
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(semNovidade, comVagaNova, alert("3")));
        when(processEmailReceiverUseCase.execute(user, LINKEDIN, List.of(semNovidade))).thenReturn(false);
        when(processEmailReceiverUseCase.execute(user, LINKEDIN, List.of(comVagaNova))).thenReturn(true);

        useCase.execute(LINKEDIN);

        verify(mailboxGateway).markAsRead(emailAccount, LINKEDIN, List.of("1"));
        verify(mailboxGateway).markAsRead(emailAccount, LINKEDIN, List.of("2"));
        verify(processEmailReceiverUseCase, times(2)).execute(any(), any(), any());
        assertTrue(statusRepository.find(user.id(), LINKEDIN).isWaiting(LocalDateTime.now()));
    }

    @Test
    void buscaSoAlertasDosUltimos7Dias() {
        user(emailAccount);
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of());

        useCase.execute(LINKEDIN);

        verify(mailboxGateway).fetchUnreadJobAlerts(emailAccount, LINKEDIN, LocalDate.now().minusDays(7));
    }

    @Test
    void duranteOIntervaloNemAbreOEmail() {
        var user = user(emailAccount);
        statusRepository.save(user.id(), LINKEDIN, statusRepository.find(user.id(), LINKEDIN).waitUntil(LocalDateTime.now().plusMinutes(10)));

        useCase.execute(LINKEDIN);

        verifyNoInteractions(mailboxGateway, processEmailReceiverUseCase);
        assertNotNull(statusRepository.find(user.id(), LINKEDIN).lastCheckAt());
    }

    @Test
    void depoisDoIntervaloProcessaOProximo() throws Exception {
        var user = user(emailAccount);
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(alert("1"), alert("2")), List.of(alert("2")));
        when(processEmailReceiverUseCase.execute(eq(user), any(), any())).thenReturn(true);

        useCase.execute(LINKEDIN);
        useCase.execute(LINKEDIN);   // ainda no intervalo
        verify(processEmailReceiverUseCase, times(1)).execute(any(), any(), any());

        endInterval(user);
        useCase.execute(LINKEDIN);

        verify(mailboxGateway).markAsRead(emailAccount, LINKEDIN, List.of("1"));
        verify(mailboxGateway).markAsRead(emailAccount, LINKEDIN, List.of("2"));
        assertEquals(0, statusRepository.find(user.id(), LINKEDIN).queuedAlerts());
    }

    @Test
    void falhaTambemRespeitaOIntervaloEFicaRegistrada() throws Exception {
        var user = user(emailAccount);
        var falha = alert("falha");
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(falha));
        doThrow(new ExternalServiceException("O LinkedIn pediu uma verificação", ErrorCode.LINKEDIN_CHECKPOINT)).when(processEmailReceiverUseCase).execute(user, LINKEDIN, List.of(falha));

        useCase.execute(LINKEDIN);

        verify(mailboxGateway, never()).markAsRead(any(), any(), anyList());
        var status = statusRepository.find(user.id(), LINKEDIN);
        assertEquals("O LinkedIn pediu uma verificação", status.lastError());
        assertEquals(ErrorCode.LINKEDIN_CHECKPOINT, status.lastErrorCode());
        assertTrue(status.isWaiting(LocalDateTime.now()));
    }

    @Test
    void erroInesperadoNaoVazaParaATela() throws Exception {
        var user = user(emailAccount);
        var alerta = alert("1");
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(alerta));
        doThrow(new NullPointerException("Cannot invoke String.length() because x is null")).when(processEmailReceiverUseCase).execute(user, LINKEDIN, List.of(alerta));

        useCase.execute(LINKEDIN);

        var status = statusRepository.find(user.id(), LINKEDIN);
        assertFalse(status.lastError().contains("NullPointer") || status.lastError().contains("Cannot invoke"), status.lastError());
        assertNull(status.lastErrorCode());
    }

    @Test
    void sucessoDepoisDeUmaFalhaLimpaOErro() {
        var user = user(emailAccount);
        statusRepository.save(user.id(), LINKEDIN, statusRepository.find(user.id(), LINKEDIN).failed(new RuntimeException("erro antigo")));
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenReturn(List.of(alert("ok")));

        useCase.execute(LINKEDIN);

        assertNull(statusRepository.find(user.id(), LINKEDIN).lastError());
    }

    @Test
    void falhaAoLerOEmailFicaRegistrada() {
        var user = user(emailAccount);
        when(mailboxGateway.fetchUnreadJobAlerts(eq(emailAccount), eq(LINKEDIN), any())).thenThrow(new ExternalServiceException("Falha ao acessar o email"));

        assertThrows(ExternalServiceException.class, () -> useCase.execute(LINKEDIN));

        assertEquals("Falha ao acessar o email", statusRepository.find(user.id(), LINKEDIN).lastError());
    }

    @Test
    void indeedSemPastaEscolhidaNaoEhLido() {
        user(emailAccount);

        useCase.execute(INDEED);

        verifyNoInteractions(mailboxGateway, processEmailReceiverUseCase);
    }

    @Test
    void indeedAnalisaOsEmailsDaFilaJuntosEMarcaCadaUmComoLido() throws Exception {
        var comIndeed = emailAccount.withFolder(INDEED, "Indeed");
        var user = user(comIndeed);
        var emails = List.of(alert("1"), alert("2"), alert("3"));
        when(mailboxGateway.fetchUnreadJobAlerts(eq(comIndeed), eq(INDEED), any())).thenReturn(emails);
        when(processEmailReceiverUseCase.execute(user, INDEED, emails)).thenReturn(true);

        useCase.execute(INDEED);

        verify(processEmailReceiverUseCase, times(1)).execute(any(), any(), any());
        verify(mailboxGateway).markAsRead(comIndeed, INDEED, List.of("1", "2", "3"));
        assertTrue(statusRepository.find(user.id(), INDEED).isWaiting(LocalDateTime.now()));
        assertFalse(statusRepository.find(user.id(), LINKEDIN).isWaiting(LocalDateTime.now()), "o intervalo do Indeed não trava o LinkedIn");
    }

    @Test
    void intervaloSorteadoFicaEntre15E25Minutos() {
        for (int i = 0; i < 200; i++) {
            Duration interval = PollJobAlertsUseCaseImpl.nextInterval();
            assertFalse(interval.compareTo(Duration.ofMinutes(15)) < 0, interval.toString());
            assertFalse(interval.compareTo(Duration.ofMinutes(25)) > 0, interval.toString());
        }
    }
}
