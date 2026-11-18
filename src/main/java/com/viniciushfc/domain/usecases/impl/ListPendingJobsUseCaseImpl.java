package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ListPendingJobsUseCase;

import java.util.List;

@UseCase
public class ListPendingJobsUseCaseImpl implements ListPendingJobsUseCase {

    private final ProfileRepository profileRepository;
    private final PendingJobRepository pendingJobRepository;

    public ListPendingJobsUseCaseImpl(ProfileRepository profileRepository, PendingJobRepository pendingJobRepository) {
        this.profileRepository = profileRepository;
        this.pendingJobRepository = pendingJobRepository;
    }

    @Override
    public List<PendingJob> execute() {
        return pendingJobRepository.findByProfileId(profileRepository.require().id());
    }
}
