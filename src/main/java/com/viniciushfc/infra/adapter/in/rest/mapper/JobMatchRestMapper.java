package com.viniciushfc.infra.adapter.in.rest.mapper;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.infra.adapter.in.rest.dto.JobMatchResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.PendingJobResponse;

public class JobMatchRestMapper {

    public static JobMatchResponse toResponse(JobMatch jobMatch) {
        return new JobMatchResponse(
                jobMatch.id(),
                jobMatch.jobLink(),
                jobMatch.jobTitle(),
                jobMatch.companyName(),
                jobMatch.match(),
                jobMatch.score(),
                jobMatch.motivos(),
                jobMatch.curriculoAts(),
                jobMatch.createdAt(),
                jobMatch.jobDescription() != null
        );
    }

    public static PendingJobResponse toResponse(PendingJob pendingJob) {
        return new PendingJobResponse(
                pendingJob.id(),
                pendingJob.source(),
                pendingJob.link(),
                pendingJob.job().title(),
                pendingJob.companyName(),
                pendingJob.prioritized(),
                pendingJob.attempts(),
                pendingJob.createdAt()
        );
    }
}
