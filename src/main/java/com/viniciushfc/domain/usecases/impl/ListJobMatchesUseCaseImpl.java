package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ListJobMatchesUseCase;

import java.util.List;

@UseCase
public class ListJobMatchesUseCaseImpl implements ListJobMatchesUseCase {

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;

    public ListJobMatchesUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
    }

    @Override
    public List<JobMatch> execute() {
        return jobMatchRepository.findByProfileId(profileRepository.require().id());
    }
}
