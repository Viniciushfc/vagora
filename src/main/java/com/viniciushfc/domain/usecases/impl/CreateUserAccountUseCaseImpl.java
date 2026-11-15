package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.contracts.CreateUserAccountUseCase;

@UseCase
public class CreateUserAccountUseCaseImpl implements CreateUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    public CreateUserAccountUseCaseImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserAccount execute(UserAccount userAccount) {
        return userAccountRepository.save(UserAccount.create(
                userAccount.name(),
                userAccount.username(),
                userAccount.password(),
                userAccount.curriculum()
        ));
    }
}
