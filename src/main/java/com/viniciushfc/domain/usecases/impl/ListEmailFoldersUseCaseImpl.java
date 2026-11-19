package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ListEmailFoldersUseCase;
import com.viniciushfc.domain.vo.MailFolder;

import java.util.List;

@UseCase
public class ListEmailFoldersUseCaseImpl implements ListEmailFoldersUseCase {

    private final ProfileRepository profileRepository;
    private final MailboxGateway mailboxGateway;

    public ListEmailFoldersUseCaseImpl(ProfileRepository profileRepository, MailboxGateway mailboxGateway) {
        this.profileRepository = profileRepository;
        this.mailboxGateway = mailboxGateway;
    }

    @Override
    public List<MailFolder> execute() {
        Profile profile = profileRepository.require();

        LinkedAccount emailAccount = profile.findLinkedAccount(ExternalProvider.EMAIL);
        if (emailAccount == null) {
            throw new BusinessException("Conecte um email em Configurações para listar as pastas.");
        }

        return mailboxGateway.listFolders(emailAccount);
    }
}
