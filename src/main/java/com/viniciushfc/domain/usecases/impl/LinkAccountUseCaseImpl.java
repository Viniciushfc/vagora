package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.LinkAccountUseCase;
import com.viniciushfc.domain.exceptions.ConflictException;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.domain.vo.MailFolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UseCase
public class LinkAccountUseCaseImpl implements LinkAccountUseCase {

    static final String DEFAULT_FOLDER = "INBOX";

    private final ProfileRepository profileRepository;
    private final MailboxGateway mailboxGateway;

    public LinkAccountUseCaseImpl(ProfileRepository profileRepository, MailboxGateway mailboxGateway) {
        this.profileRepository = profileRepository;
        this.mailboxGateway = mailboxGateway;
    }

    @Override
    public Profile execute(LinkedAccount linkedAccount) {
        validate(linkedAccount);

        Profile profile = profileRepository.require();
        if (profile.findLinkedAccount(linkedAccount.externalProvider()) != null) {
            throw new ConflictException("Já existe uma conta de " + linkedAccount.externalProvider().label
                    + " conectada. Desconecte a atual para trocar.");
        }

        LinkedAccount newAccount = LinkedAccount.create(
                linkedAccount.username(),
                linkedAccount.password(),
                linkedAccount.externalProvider(),
                null
        );

        if (linkedAccount.externalProvider() == ExternalProvider.EMAIL) {
            newAccount = newAccount.withFolder(resolveFolder(newAccount, linkedAccount.folder()));
        }

        List<LinkedAccount> linkedAccounts = new ArrayList<>(profile.linkedAccounts());
        linkedAccounts.add(newAccount);

        return profileRepository.updateLinkedAccounts(profile.id(), linkedAccounts);
    }

    private void validate(LinkedAccount linkedAccount) {
        if (Utils.isNull(linkedAccount) || Utils.isNull(linkedAccount.externalProvider())) {
            throw new ValidationException("Informe qual conta vincular: EMAIL ou LINKEDIN.");
        }
        if (isBlank(linkedAccount.username())) {
            throw new ValidationException("Informe o login da conta.");
        }
        if (isBlank(linkedAccount.password())) {
            throw new ValidationException("Informe a senha da conta.");
        }
    }

    private boolean isBlank(String value) {
        return Utils.isNull(value) || value.isBlank();
    }

    private String resolveFolder(LinkedAccount emailAccount, String requestedFolder) {
        List<MailFolder> folders = mailboxGateway.listFolders(emailAccount);

        if (!Utils.isNull(requestedFolder) && !requestedFolder.isBlank()) {
            if (folders.stream().noneMatch(f -> f.name().equals(requestedFolder))) {
                throw new ValidationException("Pasta não encontrada no email: " + requestedFolder);
            }
            return requestedFolder;
        }

        return folders.stream()
                .filter(f -> f.jobAlerts() > 0)
                .max(Comparator.comparingInt(MailFolder::jobAlerts))
                .map(MailFolder::name)
                .orElse(DEFAULT_FOLDER);
    }
}
