package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;

public interface UpdateProfileUseCase {
    Profile execute(Profile changes);
}
