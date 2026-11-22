package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.MarkJobAppliedUseCase;

import java.time.LocalDateTime;
import java.util.UUID;

@UseCase
public class MarkJobAppliedUseCaseImpl implements MarkJobAppliedUseCase {

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;

    public MarkJobAppliedUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
    }

    @Override
    public JobMatch execute(UUID jobMatchId, boolean applied) {
        Profile profile = profileRepository.require();
        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId);
        if (jobMatch == null || !jobMatch.profileId().equals(profile.id())) {
            throw new NotFoundException("Vaga não encontrada.");
        }
        LocalDateTime appliedAt = applied ? (jobMatch.appliedAt() != null ? jobMatch.appliedAt() : LocalDateTime.now()) : null;
        jobMatchRepository.updateAppliedAt(jobMatch.id(), appliedAt);
        return jobMatch.withAppliedAt(appliedAt);
    }
}
