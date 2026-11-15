package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.GetProfileUseCase;

@UseCase
public class GetProfileUseCaseImpl implements GetProfileUseCase {

    private final ProfileRepository profileRepository;

    public GetProfileUseCaseImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Profile execute() {
        return profileRepository.require();
    }
}
