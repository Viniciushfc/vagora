package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.JobMatch;

import java.util.List;
import java.util.UUID;

public interface ListJobMatchesUseCase {
    List<JobMatch> execute(UUID userAccountId);
}
