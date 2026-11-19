package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.vo.Attachment;

import java.util.UUID;

public interface GenerateCurriculumUseCase {
    JobMatch execute(UUID jobMatchId);

    Attachment pdf(UUID jobMatchId);
}
