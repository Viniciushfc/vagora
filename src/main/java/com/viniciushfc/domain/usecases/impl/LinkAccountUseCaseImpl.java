package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.contracts.LinkAccountUseCase;
import com.viniciushfc.domain.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UseCase
public class LinkAccountUseCaseImpl implements LinkAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    public LinkAccountUseCaseImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserAccount execute(UUID userAccountId, LinkedAccount linkedAccount) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId);

        if (Utils.isNull(userAccount)) {
            throw new IllegalArgumentException("Usuário não encontrado: " + userAccountId);
        }

        if (!Utils.isNull(userAccountRepository.findByLinkedUsername(linkedAccount.username()))) {
            throw new IllegalArgumentException("Conta já vinculada: " + linkedAccount.username());
        }

        List<LinkedAccount> linkedAccounts = new ArrayList<>(userAccount.linkedAccounts());
        linkedAccounts.add(LinkedAccount.create(
                linkedAccount.username(),
                linkedAccount.password(),
                linkedAccount.externalProvider()
        ));

        return userAccountRepository.update(userAccountId,
                new UserAccount(userAccountId, null, null, null, null, linkedAccounts));
    }
}
