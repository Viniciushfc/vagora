package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.PendingJob;

import java.util.List;

public interface ListPendingJobsUseCase {
    List<PendingJob> execute();
}
