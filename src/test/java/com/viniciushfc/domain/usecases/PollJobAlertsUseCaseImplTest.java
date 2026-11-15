package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        Profile profile = new Profile(UUID.randomUUID(), "F", "k", null, new ArrayList<>(List.of(accounts)), null, null);
        lenient().when(profileRepository.find()).thenReturn(profile);
        return profile;
    }

    private EmailReceived alert(String messageId) {
        return EmailReceived.create(messageId, new Email("jobalerts-noreply@linkedin.com", "f@gmail.com", "Alerta " + messageId, "b", List.of()));
    }

    private void endInterval(Profile user) {
        statusRepository.save(user.id(), statusRepository.find(user.id()).waitUntil(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void monitoramentoPausadoNaoAbreOEmail() {
        var pausado = new Profile(UUID.randomUUID(), "F", "k", null, new ArrayList<>(List.of(emailAccount)), null, false);
        when(profileRepository.find()).thenReturn(pausado);

        useCase.execute();

        verifyNoInteractions(mailboxGateway, processEmailReceiverUseCase);
    }

    @Test
    void processaUmAlertaPorVezEAgendaOProximoComPeloMenos15Minutos() throws Exception {
        var user = user(emailAccount);
        var primeiro = alert("1");
        when(mailboxGateway.fetchUnreadJobAlerts(emailAccount)).thenReturn(List.of(primeiro, alert("2"), alert("3")));

        var antes = LocalDateTime.now();
        useCase.execute();

        verify(processEmailReceiverUseCase, times(1)).execute(any(), any());
        verify(processEmailReceiverUseCase).execute(user, primeiro);
        verify(mailboxGateway).markAsRead(emailAccount, "1");

        var status = statusRepository.find(user.id());
        assertEquals(2, status.queuedAlerts());
        assertNull(status.currentAlert());
        assertFalse(status.nextAlertAt().isBefore(antes.plus(PollJobAlertsUseCaseImpl.MIN_INTERVAL)));
        assertFalse(status.nextAlertAt().isAfter(LocalDateTime.now().plus(PollJobAlertsUseCaseImpl.MIN_INTERVAL).plus(PollJobAlertsUseCaseImpl.MAX_JITTER)));
    }

    @Test
    void duranteOIntervaloNemAbreOEmail() {
        var user = user(emailAccount);
        statusRepository.save(user.id(), statusRepository.find(user.id()).waitUntil(LocalDateTime.now().plusMinutes(10)));

        useCase.execute();

        verifyNoInteractions(mailboxGateway, processEmailReceiverUseCase);
        assertNotNull(statusRepository.find(user.id()).lastCheckAt());
    }

    @Test
    void depoisDoIntervaloProcessaOProximo() throws Exception {
        var user = user(emailAccount);
        when(mailboxGateway.fetchUnreadJobAlerts(emailAccount)).thenReturn(List.of(alert("1"), alert("2")), List.of(alert("2")));

        useCase.execute();
        useCase.execute();   // ainda no intervalo
        verify(processEmailReceiverUseCase, times(1)).execute(any(), any());

        endInterval(user);
        useCase.execute();

        verify(mailboxGateway).markAsRead(emailAccount, "1");
        verify(mailboxGateway).markAsRead(emailAccount, "2");
        assertEquals(0, statusRepository.find(user.id()).queuedAlerts());
    }

    @Test
    void falhaTambemRespeitaOIntervaloEFicaRegistrada() throws Exception {
        var user = user(emailAccount);
        var falha = alert("falha");
        when(mailboxGateway.fetchUnreadJobAlerts(emailAccount)).thenReturn(List.of(falha));
        doThrow(new ExternalServiceException("O LinkedIn pediu uma verificação", ErrorCode.LINKEDIN_CHECKPOINT)).when(processEmailReceiverUseCase).execute(user, falha);

        useCase.execute();

        verify(mailboxGateway, never()).markAsRead(any(), any());
        var status = statusRepository.find(user.id());
        assertEquals("O LinkedIn pediu uma verificação", status.lastError());
        assertEquals(ErrorCode.LINKEDIN_CHECKPOINT, status.lastErrorCode());
        assertTrue(status.isWaiting(LocalDateTime.now()));
    }

    @Test
    void sucessoDepoisDeUmaFalhaLimpaOErro() {
        var user = user(emailAccount);
        statusRepository.save(user.id(), statusRepository.find(user.id()).failed(new RuntimeException("erro antigo")));
        when(mailboxGateway.fetchUnreadJobAlerts(emailAccount)).thenReturn(List.of(alert("ok")));

        useCase.execute();

        assertNull(statusRepository.find(user.id()).lastError());
    }

    @Test
    void falhaAoLerOEmailFicaRegistrada() {
        var user = user(emailAccount);
        when(mailboxGateway.fetchUnreadJobAlerts(emailAccount)).thenThrow(new ExternalServiceException("Falha ao acessar o email"));

        assertThrows(ExternalServiceException.class, () -> useCase.execute());

        assertEquals("Falha ao acessar o email", statusRepository.find(user.id()).lastError());
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
