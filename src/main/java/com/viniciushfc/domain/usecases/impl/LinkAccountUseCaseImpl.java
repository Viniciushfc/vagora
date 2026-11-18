package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.LinkAccountUseCase;
import com.viniciushfc.domain.exceptions.ConflictException;
import com.viniciushfc.domain.vo.MailFolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
            List<MailFolder> folders = mailboxGateway.listFolders(newAccount);
            newAccount = newAccount.withFolder(AlertSource.LINKEDIN, resolveFolder(folders, linkedAccount.folder()))
                    .withFolder(AlertSource.INDEED, mostAlerts(folders, AlertSource.INDEED).orElse(null));
        }

        List<LinkedAccount> linkedAccounts = new ArrayList<>(profile.linkedAccounts());
        linkedAccounts.add(newAccount);

        return profileRepository.updateLinkedAccounts(profile.id(), linkedAccounts);
    }

    private void validate(LinkedAccount linkedAccount) {
        if (linkedAccount == null || linkedAccount.externalProvider() == null) {
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
        return value == null || value.isBlank();
    }

    private String resolveFolder(List<MailFolder> folders, String requestedFolder) {
        if (requestedFolder != null && !requestedFolder.isBlank()) {
            if (folders.stream().noneMatch(f -> f.name().equals(requestedFolder))) {
                throw new ValidationException("Pasta não encontrada no email: " + requestedFolder);
            }
            return requestedFolder;
        }

        return mostAlerts(folders, AlertSource.LINKEDIN).orElse(DEFAULT_FOLDER);
    }

    private Optional<String> mostAlerts(List<MailFolder> folders, AlertSource source) {
        return folders.stream()
                .filter(f -> f.alerts(source) > 0)
                .max(Comparator.comparingInt(f -> f.alerts(source)))
                .map(MailFolder::name);
    }
}
