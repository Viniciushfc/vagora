package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.exceptions.*;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.impl.*;
import com.viniciushfc.domain.validators.CurriculumValidator;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.MailFolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileUseCasesTest {

    @Mock
    ProfileRepository profileRepository;
    @Mock
    MailboxGateway mailboxGateway;

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1});

    private Profile profile(LinkedAccount... accounts) {
        return new Profile(UUID.randomUUID(), "Fulano", "chave", cv, new ArrayList<>(List.of(accounts)), null, null);
    }

    private Profile existing(LinkedAccount... accounts) {
        Profile profile = profile(accounts);
        when(profileRepository.require()).thenReturn(profile);
        return profile;
    }

    private LinkedAccount request(String login, ExternalProvider provider, String folder) {
        return new LinkedAccount(null, login, "senha", provider, folder);
    }

    @SuppressWarnings("unchecked")
    private List<LinkedAccount> savedAccounts(Profile profile) {
        ArgumentCaptor<List<LinkedAccount>> captor = ArgumentCaptor.forClass(List.class);
        verify(profileRepository).updateLinkedAccounts(eq(profile.id()), captor.capture());
        return captor.getValue();
    }

    @Test
    void createGeraIdEMantemChaveDoGemini() {
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Profile created = new CreateProfileUseCaseImpl(profileRepository)
                .execute(new Profile(null, "Fulano", "chave", cv, null, null, null));

        assertNotNull(created.id());
        assertEquals("chave", created.geminiApiKey());
        assertTrue(created.linkedAccounts().isEmpty());
        assertTrue(created.isMonitoringEnabled());
    }

    @Test
    void createRecusaUmSegundoPerfil() {
        when(profileRepository.find()).thenReturn(profile());

        var useCase = new CreateProfileUseCaseImpl(profileRepository);
        assertThrows(ConflictException.class, () -> useCase.execute(new Profile(null, "Outro", "k", cv, null, null, null)));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void createRejeitaCurriculoInvalido() {
        var useCase = new CreateProfileUseCaseImpl(profileRepository);
        var semCv = new Profile(null, "F", "k", null, null, null, null);
        var naoPdf = new Profile(null, "F", "k", new Attachment("cv.docx", "application/msword", new byte[]{1}), null, null, null);
        var grande = new Profile(null, "F", "k",
                new Attachment("cv.pdf", "application/pdf", new byte[CurriculumValidator.MAX_SIZE_BYTES + 1]), null, null, null);

        assertThrows(ValidationException.class, () -> useCase.execute(semCv));
        assertThrows(ValidationException.class, () -> useCase.execute(naoPdf));
        var e = assertThrows(ValidationException.class, () -> useCase.execute(grande));
        assertTrue(e.getMessage().contains("limite é 5.0 MB"), e.getMessage());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateAplicaNoPerfilDaInstalacaoEValidaCurriculo() {
        Profile current = existing();
        var useCase = new UpdateProfileUseCaseImpl(profileRepository);

        var naoPdf = new Profile(null, null, null, new Attachment("cv.txt", "text/plain", new byte[]{1}), null, null, null);
        assertThrows(ValidationException.class, () -> useCase.execute(naoPdf));
        verify(profileRepository, never()).update(any(), any());

        var soNome = new Profile(null, "Novo", null, null, null, null, null);
        when(profileRepository.update(current.id(), soNome)).thenReturn(current);
        assertEquals(current, useCase.execute(soNome));
    }

    @Test
    void linkRejeitaCamposVaziosSemConsultarNada() {
        var useCase = new LinkAccountUseCaseImpl(profileRepository, mailboxGateway);

        assertThrows(ValidationException.class, () -> useCase.execute(new LinkedAccount(null, null, "s", ExternalProvider.EMAIL, null)));
        assertThrows(ValidationException.class, () -> useCase.execute(new LinkedAccount(null, " ", "s", ExternalProvider.EMAIL, null)));
        assertThrows(ValidationException.class, () -> useCase.execute(new LinkedAccount(null, "u", "", ExternalProvider.EMAIL, null)));
        assertThrows(ValidationException.class, () -> useCase.execute(new LinkedAccount(null, "u", "s", null, null)));
        verifyNoInteractions(profileRepository, mailboxGateway);
    }

    @Test
    void linkLinkedinNaoAcessaEmail() {
        Profile current = existing();

        new LinkAccountUseCaseImpl(profileRepository, mailboxGateway).execute(request("login", ExternalProvider.LINKEDIN, null));

        var linked = savedAccounts(current);
        assertEquals(1, linked.size());
        assertNotNull(linked.getFirst().id());
        assertNull(linked.getFirst().folder());
        verifyNoInteractions(mailboxGateway);
    }

    @Test
    void linkRecusaSegundaContaDoMesmoProviderSemTestarOEmail() {
        existing(LinkedAccount.create("f@gmail.com", "app", ExternalProvider.EMAIL, "INBOX"));

        var useCase = new LinkAccountUseCaseImpl(profileRepository, mailboxGateway);
        var e = assertThrows(ConflictException.class, () -> useCase.execute(request("outro@gmail.com", ExternalProvider.EMAIL, null)));
        assertEquals("Já existe uma conta de Email conectada. Desconecte a atual para trocar.", e.getMessage());
        verifyNoInteractions(mailboxGateway);
        verify(profileRepository, never()).updateLinkedAccounts(any(), any());
    }

    @Test
    void unlinkRemoveSoAContaDoProvider() {
        var email = LinkedAccount.create("f@gmail.com", "app", ExternalProvider.EMAIL, "INBOX");
        var linkedin = LinkedAccount.create("login", "x", ExternalProvider.LINKEDIN, null);
        Profile current = existing(email, linkedin);

        new UnlinkAccountUseCaseImpl(profileRepository).execute(ExternalProvider.LINKEDIN);

        assertEquals(List.of(email), savedAccounts(current));
    }

    @Test
    void linkEmailDetectaPastaComMaisAlertas() {
        Profile current = existing();
        when(mailboxGateway.listFolders(any())).thenReturn(List.of(
                new MailFolder("INBOX", 0), new MailFolder("LinkedIn", 12), new MailFolder("Spam", 1)));

        new LinkAccountUseCaseImpl(profileRepository, mailboxGateway).execute(request("f@gmail.com", ExternalProvider.EMAIL, null));

        assertEquals("LinkedIn", savedAccounts(current).getFirst().folder());
    }

    @Test
    void linkEmailUsaInboxQuandoNaoHaAlertas() {
        Profile current = existing();
        when(mailboxGateway.listFolders(any())).thenReturn(List.of(new MailFolder("INBOX", 0), new MailFolder("Outros", 0)));

        new LinkAccountUseCaseImpl(profileRepository, mailboxGateway).execute(request("f@gmail.com", ExternalProvider.EMAIL, null));

        assertEquals("INBOX", savedAccounts(current).getFirst().folder());
    }

    @Test
    void linkEmailComPastaInformadaValidaSeExiste() {
        existing();
        when(mailboxGateway.listFolders(any())).thenReturn(List.of(new MailFolder("INBOX", 0)));

        var useCase = new LinkAccountUseCaseImpl(profileRepository, mailboxGateway);
        assertThrows(ValidationException.class, () -> useCase.execute(request("f@gmail.com", ExternalProvider.EMAIL, "NaoExiste")));
        verify(profileRepository, never()).updateLinkedAccounts(any(), any());
    }

    @Test
    void listEmailFoldersExigeContaDeEmail() {
        existing();

        var useCase = new ListEmailFoldersUseCaseImpl(profileRepository, mailboxGateway);
        assertThrows(BusinessException.class, useCase::execute);
    }

    @Test
    void updateEmailFolderTrocaSomenteAPastaDoEmail() {
        var email = LinkedAccount.create("f@gmail.com", "app", ExternalProvider.EMAIL, "INBOX");
        var linkedin = LinkedAccount.create("login", "x", ExternalProvider.LINKEDIN, null);
        Profile current = existing(email, linkedin);
        when(mailboxGateway.listFolders(email)).thenReturn(List.of(new MailFolder("INBOX", 0), new MailFolder("LinkedIn", 5)));

        new UpdateEmailFolderUseCaseImpl(profileRepository, mailboxGateway).execute("LinkedIn");

        var linked = savedAccounts(current);
        assertEquals("LinkedIn", linked.get(0).folder());
        assertEquals(email.id(), linked.get(0).id());
        assertEquals(linkedin, linked.get(1));
    }

    @Test
    void updateEmailFolderRejeitaPastaInexistente() {
        var email = LinkedAccount.create("f@gmail.com", "app", ExternalProvider.EMAIL, "INBOX");
        existing(email);
        when(mailboxGateway.listFolders(email)).thenReturn(List.of(new MailFolder("INBOX", 0)));

        var useCase = new UpdateEmailFolderUseCaseImpl(profileRepository, mailboxGateway);
        assertThrows(ValidationException.class, () -> useCase.execute("Nada"));
    }
}
