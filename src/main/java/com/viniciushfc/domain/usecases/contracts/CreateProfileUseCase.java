package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;

public interface CreateProfileUseCase {
    Profile execute(Profile profile);
}
