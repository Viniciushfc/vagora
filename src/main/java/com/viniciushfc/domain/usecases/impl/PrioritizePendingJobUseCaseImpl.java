package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.PrioritizePendingJobUseCase;

import java.util.UUID;

@UseCase
public class PrioritizePendingJobUseCaseImpl implements PrioritizePendingJobUseCase {

    private final ProfileRepository profileRepository;
    private final PendingJobRepository pendingJobRepository;

    public PrioritizePendingJobUseCaseImpl(ProfileRepository profileRepository, PendingJobRepository pendingJobRepository) {
        this.profileRepository = profileRepository;
        this.pendingJobRepository = pendingJobRepository;
    }

    @Override
    public PendingJob execute(UUID pendingJobId) {
        Profile profile = profileRepository.require();
        PendingJob pendingJob = pendingJobRepository.findById(pendingJobId);
        if (pendingJob == null || !pendingJob.profileId().equals(profile.id())) {
            throw new NotFoundException("Esta vaga não está mais na fila: talvez já tenha sido analisada.");
        }
        pendingJobRepository.prioritize(pendingJobId);
        return pendingJobRepository.findById(pendingJobId);
    }
}
