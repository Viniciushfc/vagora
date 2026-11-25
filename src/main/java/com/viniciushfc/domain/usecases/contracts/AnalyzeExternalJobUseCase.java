package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.vo.JobDetail;

public interface AnalyzeExternalJobUseCase {
    JobMatch execute(JobDetail job);
}
