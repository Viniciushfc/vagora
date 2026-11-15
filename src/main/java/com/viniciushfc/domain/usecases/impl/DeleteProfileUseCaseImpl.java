package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.DeleteProfileUseCase;

@UseCase
public class DeleteProfileUseCaseImpl implements DeleteProfileUseCase {

    private final ProfileRepository profileRepository;

    public DeleteProfileUseCaseImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void execute() {
        profileRepository.delete(profileRepository.require().id());
    }
}
