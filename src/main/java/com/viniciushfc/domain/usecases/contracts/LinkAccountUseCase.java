package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;

public interface LinkAccountUseCase {
    Profile execute(LinkedAccount linkedAccount);
}
