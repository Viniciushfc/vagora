package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JobMatch(UUID id,
                       UUID profileId,
                       UUID emailReceivedId,
                       String jobLink,
                       String jobTitle,
                       String companyName,
                       boolean match,
                       int score,
                       List<String> motivos,
                       String curriculoAts,
                       LocalDateTime createdAt) {

    public static JobMatch create(UUID profileId, UUID emailReceivedId, JobDetail jobDetail, CvMatchResult result) {
        return new JobMatch(
                UUID.randomUUID(),
                profileId,
                emailReceivedId,
                jobDetail.link(),
                jobDetail.title(),
                jobDetail.companyDetails().name(),
                result.match(),
                result.score(),
                result.motivos(),
                result.curriculoAts(),
                LocalDateTime.now()
        );
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JobMatch other && id.equals(other.id);
    }
}
