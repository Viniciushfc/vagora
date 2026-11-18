package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.JobDetail;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingJob(UUID id,
                         UUID profileId,
                         UUID emailReceivedId,
                         AlertSource source,
                         JobDetail job,
                         boolean prioritized,
                         int attempts,
                         LocalDateTime createdAt) {

    public static PendingJob create(UUID profileId, UUID emailReceivedId, AlertSource source, JobDetail job) {
        return new PendingJob(UUID.randomUUID(), profileId, emailReceivedId, source, job, false, 0, LocalDateTime.now());
    }

    public String link() {
        return job.link();
    }

    public String companyName() {
        CompanyDetails company = job.companyDetails();
        return company == null ? null : company.name();
    }
}
