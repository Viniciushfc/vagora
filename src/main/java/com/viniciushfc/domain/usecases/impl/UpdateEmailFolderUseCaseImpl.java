package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateEmailFolderUseCase;
import com.viniciushfc.domain.utils.Utils;

import java.util.List;

@UseCase
public class UpdateEmailFolderUseCaseImpl implements UpdateEmailFolderUseCase {

    private final ProfileRepository profileRepository;
    private final MailboxGateway mailboxGateway;

    public UpdateEmailFolderUseCaseImpl(ProfileRepository profileRepository, MailboxGateway mailboxGateway) {
        this.profileRepository = profileRepository;
        this.mailboxGateway = mailboxGateway;
    }

    @Override
    public Profile execute(AlertSource source, String folder) {
        Profile profile = profileRepository.require();

        LinkedAccount emailAccount = profile.findLinkedAccount(ExternalProvider.EMAIL);
        if (Utils.isNull(emailAccount)) {
            throw new BusinessException("Conecte um email em Contas para listar as pastas.");
        }

        if (Utils.isNull(folder) || folder.isBlank()) {
            if (source != AlertSource.INDEED) {
                throw new ValidationException("Escolha a pasta dos alertas do " + source.label + ".");
            }
            folder = null;
        } else if (!mailboxGateway.folderExists(emailAccount, folder)) {
            throw new ValidationException("Pasta não encontrada no email: " + folder);
        }
        String chosen = folder;

        List<LinkedAccount> linkedAccounts = profile.linkedAccounts().stream()
                .map(account -> account.id().equals(emailAccount.id()) ? account.withFolder(source, chosen) : account)
                .toList();

        return profileRepository.updateLinkedAccounts(profile.id(), linkedAccounts);
    }
}
