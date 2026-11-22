package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ListGupySearchesUseCase;

import java.util.List;

@UseCase
public class ListGupySearchesUseCaseImpl implements ListGupySearchesUseCase {

    private final ProfileRepository profileRepository;
    private final GupySearchRepository gupySearchRepository;

    public ListGupySearchesUseCaseImpl(ProfileRepository profileRepository, GupySearchRepository gupySearchRepository) {
        this.profileRepository = profileRepository;
        this.gupySearchRepository = gupySearchRepository;
    }

    @Override
    public List<GupySearch> execute() {
        return gupySearchRepository.findByProfileId(profileRepository.require().id());
    }
}
