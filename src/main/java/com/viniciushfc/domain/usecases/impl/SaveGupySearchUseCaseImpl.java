package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.SaveGupySearchUseCase;

import java.util.UUID;

@UseCase
public class SaveGupySearchUseCaseImpl implements SaveGupySearchUseCase {

    private final ProfileRepository profileRepository;
    private final GupySearchRepository gupySearchRepository;

    public SaveGupySearchUseCaseImpl(ProfileRepository profileRepository, GupySearchRepository gupySearchRepository) {
        this.profileRepository = profileRepository;
        this.gupySearchRepository = gupySearchRepository;
    }

    @Override
    public GupySearch execute(GupySearch search) {
        Profile profile = profileRepository.require();
        if (search.jobName() == null || search.jobName().isBlank()) {
            throw new ValidationException("Informe o cargo que você procura.");
        }
        if (search.workplaces() == null || search.workplaces().isEmpty()) {
            throw new ValidationException("Marque pelo menos uma modalidade: remoto, híbrido ou presencial.");
        }
        if (search.contracts() == null || search.contracts().isEmpty()) {
            throw new ValidationException("Marque pelo menos um tipo de vaga.");
        }
        if (search.everyHours() < 1) {
            throw new ValidationException("O intervalo mínimo entre as buscas é de 1 hora.");
        }

        GupySearch existing = search.id() == null ? null : gupySearchRepository.findById(search.id());
        if (search.id() != null && (existing == null || !existing.profileId().equals(profile.id()))) {
            throw new NotFoundException("Esta busca não existe mais.");
        }
        GupySearch saved = new GupySearch(existing == null ? UUID.randomUUID() : existing.id(), profile.id(),
                search.jobName().trim(), blankToNull(search.state()), blankToNull(search.city()),
                search.workplaces(), search.contracts(), search.everyHours(), search.enabled(),
                existing == null ? null : existing.lastRunAt());
        gupySearchRepository.save(saved);
        return saved;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
