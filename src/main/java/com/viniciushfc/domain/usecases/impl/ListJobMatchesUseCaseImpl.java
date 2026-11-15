package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.usecases.contracts.ListJobMatchesUseCase;

import java.util.List;
import java.util.UUID;

@UseCase
public class ListJobMatchesUseCaseImpl implements ListJobMatchesUseCase {

    private final JobMatchRepository jobMatchRepository;

    public ListJobMatchesUseCaseImpl(JobMatchRepository jobMatchRepository) {
        this.jobMatchRepository = jobMatchRepository;
    }

    @Override
    public List<JobMatch> execute(UUID userAccountId) {
        return jobMatchRepository.findByUserAccountId(userAccountId);
    }
}
