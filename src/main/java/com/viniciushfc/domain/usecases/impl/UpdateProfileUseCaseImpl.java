package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateProfileUseCase;
import com.viniciushfc.domain.validators.CurriculumValidator;

@UseCase
public class UpdateProfileUseCaseImpl implements UpdateProfileUseCase {

    private final ProfileRepository profileRepository;
    private final PdfGateway pdfGateway;

    public UpdateProfileUseCaseImpl(ProfileRepository profileRepository, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public Profile execute(Profile changes) {
        Profile current = profileRepository.require();
        if (changes.curriculum() != null) {
            CurriculumValidator.validate(changes.curriculum(), pdfGateway);
        }
        return profileRepository.update(current.id(), changes);
    }
}
