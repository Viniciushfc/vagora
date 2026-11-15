package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.impl.CreateUserAccountUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.DeleteUserAccountUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.LinkAccountUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.UpdateUserAccountUseCaseImpl;
import com.viniciushfc.domain.vo.Attachment;
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
class UserAccountUseCasesTest {

    @Mock
    UserAccountRepository userAccountRepository;

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1});

    @Test
    void createGeraIdEComecaSemContasVinculadas() {
        when(userAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var input = new UserAccount(null, "Fulano", "fulano", "123", cv, null);

        UserAccount created = new CreateUserAccountUseCaseImpl(userAccountRepository).execute(input);

        assertNotNull(created.id());
        assertEquals("Fulano", created.name());
        assertEquals("fulano", created.username());
        assertEquals(cv, created.curriculum());
        assertTrue(created.linkedAccounts().isEmpty());
    }

    @Test
    void updateRetornaUsuarioAtualizado() {
        var id = UUID.randomUUID();
        var input = new UserAccount(null, "Novo", null, null, null, null);
        var updated = new UserAccount(id, "Novo", "fulano", "123", cv, List.of());
        when(userAccountRepository.update(id, input)).thenReturn(updated);

        assertEquals(updated, new UpdateUserAccountUseCaseImpl(userAccountRepository).execute(id, input));
    }

    @Test
    void updateLancaErroQuandoUsuarioNaoExiste() {
        var id = UUID.randomUUID();
        when(userAccountRepository.update(eq(id), any())).thenReturn(null);

        var useCase = new UpdateUserAccountUseCaseImpl(userAccountRepository);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(id, new UserAccount(null, "x", null, null, null, null)));
    }

    @Test
    void deleteRemovePeloId() {
        var id = UUID.randomUUID();

        new DeleteUserAccountUseCaseImpl(userAccountRepository).execute(id);

        verify(userAccountRepository).delete(id);
    }

    @Test
    void linkAdicionaContaMantendoAsExistentes() {
        var id = UUID.randomUUID();
        var existente = LinkedAccount.create("antiga@gmail.com", "x", ExternalProvider.LINKEDIN);
        var user = new UserAccount(id, "Fulano", "fulano", "123", cv, new ArrayList<>(List.of(existente)));
        when(userAccountRepository.findById(id)).thenReturn(user);
        when(userAccountRepository.findByLinkedUsername("nova@gmail.com")).thenReturn(null);
        when(userAccountRepository.update(eq(id), any())).thenAnswer(inv -> inv.getArgument(1));

        var request = new LinkedAccount(null, "nova@gmail.com", "y", ExternalProvider.LINKEDIN);
        new LinkAccountUseCaseImpl(userAccountRepository).execute(id, request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).update(eq(id), captor.capture());
        List<LinkedAccount> linked = captor.getValue().linkedAccounts();
        assertEquals(2, linked.size());
        assertEquals(existente, linked.get(0));
        assertNotNull(linked.get(1).id());
        assertEquals("nova@gmail.com", linked.get(1).username());
        assertEquals(ExternalProvider.LINKEDIN, linked.get(1).externalProvider());
        assertNull(captor.getValue().name(), "só as contas vinculadas devem ser atualizadas");
    }

    @Test
    void linkLancaErroQuandoUsuarioNaoExiste() {
        var id = UUID.randomUUID();
        when(userAccountRepository.findById(id)).thenReturn(null);

        var useCase = new LinkAccountUseCaseImpl(userAccountRepository);
        var request = new LinkedAccount(null, "nova@gmail.com", "y", ExternalProvider.LINKEDIN);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(id, request));
        verify(userAccountRepository, never()).update(any(), any());
    }

    @Test
    void linkLancaErroQuandoContaJaVinculada() {
        var id = UUID.randomUUID();
        var user = new UserAccount(id, "Fulano", "fulano", "123", cv, new ArrayList<>());
        when(userAccountRepository.findById(id)).thenReturn(user);
        when(userAccountRepository.findByLinkedUsername("nova@gmail.com")).thenReturn(user);

        var useCase = new LinkAccountUseCaseImpl(userAccountRepository);
        var request = new LinkedAccount(null, "nova@gmail.com", "y", ExternalProvider.LINKEDIN);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(id, request));
        verify(userAccountRepository, never()).update(any(), any());
    }
}
