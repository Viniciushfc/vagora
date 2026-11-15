package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.UserAccount;

public interface CreateUserAccountUseCase {
    UserAccount execute(UserAccount userAccount);
}
