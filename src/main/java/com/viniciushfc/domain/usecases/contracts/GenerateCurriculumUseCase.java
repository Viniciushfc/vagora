package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.JobMatch;

import java.util.UUID;

public interface GenerateCurriculumUseCase {
    JobMatch execute(UUID jobMatchId);
}
