package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.DeleteGupySearchUseCase;

import java.util.UUID;

@UseCase
public class DeleteGupySearchUseCaseImpl implements DeleteGupySearchUseCase {

    private final ProfileRepository profileRepository;
    private final GupySearchRepository gupySearchRepository;

    public DeleteGupySearchUseCaseImpl(ProfileRepository profileRepository, GupySearchRepository gupySearchRepository) {
        this.profileRepository = profileRepository;
        this.gupySearchRepository = gupySearchRepository;
    }

    @Override
    public void execute(UUID id) {
        GupySearch search = gupySearchRepository.findById(id);
        if (search == null || !search.profileId().equals(profileRepository.require().id())) {
            throw new NotFoundException("Esta busca não existe mais.");
        }
        gupySearchRepository.delete(id);
    }
}
