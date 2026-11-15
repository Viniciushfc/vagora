package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.contracts.DeleteUserAccountUseCase;

import java.util.UUID;

@UseCase
public class DeleteUserAccountUseCaseImpl implements DeleteUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    public DeleteUserAccountUseCaseImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public void execute(UUID id) {
        userAccountRepository.delete(id);
    }
}
