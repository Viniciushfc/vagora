package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.JobMatch;

import java.util.List;

public interface ListJobMatchesUseCase {
    List<JobMatch> execute();
}
