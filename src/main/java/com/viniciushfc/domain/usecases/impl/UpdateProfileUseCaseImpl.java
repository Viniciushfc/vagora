package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateProfileUseCase;
import com.viniciushfc.domain.validators.CurriculumValidator;

import java.util.List;

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
        if (changes.pollMinMinutes() != null || changes.pollMaxMinutes() != null) {
            validatePollInterval(changes.pollMinMinutes() != null ? changes.pollMinMinutes() : current.pollMin(),
                    changes.pollMaxMinutes() != null ? changes.pollMaxMinutes() : current.pollMax());
        }
        validateChain(changes.analysisChain());
        validateChain(changes.curriculumChain());
        return profileRepository.update(current.id(), changes);
    }

    private static void validateChain(List<LlmProvider> chain) {
        if (chain == null) {
            return;
        }
        if (chain.isEmpty()) {
            throw new ValidationException("Marque pelo menos uma IA em cada etapa do fluxo.");
        }
        if (chain.stream().distinct().count() != chain.size()) {
            throw new ValidationException("Cada IA aparece uma vez só em cada etapa do fluxo.");
        }
    }

    private static void validatePollInterval(int min, int max) {
        if (min < 1) {
            throw new ValidationException("O intervalo mínimo é de 1 minuto.");
        }
        if (max < min) {
            throw new ValidationException("O tempo máximo precisa ser maior ou igual ao mínimo.");
        }
    }
}
