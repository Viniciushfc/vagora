package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UpdateProfileUseCase;
import com.viniciushfc.domain.validators.CurriculumValidator;
import com.viniciushfc.domain.vo.LocalLlm;

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
        validateLocalLlm(changes.localLlm(), current.localLlm());
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

    private static void validateLocalLlm(LocalLlm changes, LocalLlm current) {
        if (changes == null) {
            return;
        }
        String url = changes.url() != null ? changes.url() : current == null ? null : current.url();
        String model = changes.model() != null ? changes.model() : current == null ? null : current.model();
        if (url == null || url.isBlank()) {
            return;
        }
        if (!url.strip().matches("(?i)https?://\\S+")) {
            throw new ValidationException("O endereço da IA local precisa começar com http:// ou https://.");
        }
        if (model == null || model.isBlank()) {
            throw new ValidationException("Informe o modelo da IA local, do jeito que ele aparece no seu servidor.");
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
