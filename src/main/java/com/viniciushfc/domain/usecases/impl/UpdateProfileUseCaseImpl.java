package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateProfileUseCase;
import com.viniciushfc.domain.validators.CurriculumValidator;

@UseCase
public class UpdateProfileUseCaseImpl implements UpdateProfileUseCase {

    private final ProfileRepository profileRepository;

    public UpdateProfileUseCaseImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /** Campos nulos em {@code changes} mantêm o valor atual. */
    @Override
    public Profile execute(Profile changes) {
        Profile current = profileRepository.require();
        if (changes.curriculum() != null) {
            CurriculumValidator.validate(changes.curriculum());
        }
        return profileRepository.update(current.id(), changes);
    }
}
