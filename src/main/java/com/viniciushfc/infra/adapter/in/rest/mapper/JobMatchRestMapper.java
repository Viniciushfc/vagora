package com.viniciushfc.infra.adapter.in.rest.mapper;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.infra.adapter.in.rest.dto.JobMatchResponse;

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
                jobMatch.createdAt()
        );
    }
}
