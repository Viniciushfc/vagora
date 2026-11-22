package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.ClaudeAccountGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.impl.ConnectClaudeUseCaseImpl;
import com.viniciushfc.domain.vo.ClaudeStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectClaudeUseCaseImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void jaLogadoTrocaAContaAntigaPelaAtualSemAbrirLogin() {
        ProfileRepository repository = mock(ProfileRepository.class);
        ClaudeAccountGateway gateway = mock(ClaudeAccountGateway.class);
        Profile profile = Profile.create("F", "k", null, null, null);
        var email = LinkedAccount.create("e@x.com", "s", ExternalProvider.EMAIL, "INBOX");
        profile.linkedAccounts().addAll(List.of(email, LinkedAccount.create("velho@x.com", null, ExternalProvider.CLAUDE, null)));
        when(repository.require()).thenReturn(profile);
        when(gateway.status()).thenReturn(new ClaudeStatus(true, true, "novo@x.com", "pro", false, null, null, false, null));

        new ConnectClaudeUseCaseImpl(repository, gateway).execute(null);

        ArgumentCaptor<List<LinkedAccount>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).updateLinkedAccounts(eq(profile.id()), saved.capture());
        assertEquals(List.of(email.username(), "novo@x.com"), saved.getValue().stream().map(LinkedAccount::username).toList());
        verify(gateway, never()).login(any(), any());
    }
}
