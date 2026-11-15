package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;

import java.util.UUID;

public interface LinkAccountUseCase {
    UserAccount execute(UUID userAccountId, LinkedAccount linkedAccount);
}
