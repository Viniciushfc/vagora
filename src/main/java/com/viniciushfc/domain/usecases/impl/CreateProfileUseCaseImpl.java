package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.ConflictException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.CreateProfileUseCase;
import com.viniciushfc.domain.validators.CurriculumValidator;

@UseCase
public class CreateProfileUseCaseImpl implements CreateProfileUseCase {

    private final ProfileRepository profileRepository;
    private final PdfGateway pdfGateway;

    public CreateProfileUseCaseImpl(ProfileRepository profileRepository, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public Profile execute(Profile profile) {
        if (profileRepository.find() != null) {
            throw new ConflictException("Já existe um perfil nesta instalação.");
        }
        CurriculumValidator.validate(profile.curriculum(), pdfGateway);

        Profile created = Profile.create(
                profile.name(),
                profile.geminiApiKey(),
                profile.groqApiKey(),
                profile.llmProvider(),
                profile.curriculum());
        return profileRepository.save(created);
    }
}
