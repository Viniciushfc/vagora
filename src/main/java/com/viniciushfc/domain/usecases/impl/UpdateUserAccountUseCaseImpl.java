package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateUserAccountUseCase;
import com.viniciushfc.domain.utils.Utils;

import java.util.UUID;

@UseCase
public class UpdateUserAccountUseCaseImpl implements UpdateUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    public UpdateUserAccountUseCaseImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserAccount execute(UUID id, UserAccount userAccount) {
        UserAccount updated = userAccountRepository.update(id, userAccount);

        if (Utils.isNull(updated)) {
            throw new IllegalArgumentException("Usuário não encontrado: " + id);
        }

        return updated;
    }
}
