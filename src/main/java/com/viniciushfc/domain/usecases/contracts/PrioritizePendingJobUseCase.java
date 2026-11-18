package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.PendingJob;

import java.util.UUID;

public interface PrioritizePendingJobUseCase {
    PendingJob execute(UUID pendingJobId);
}
