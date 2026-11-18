package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanachePendingJobEntity;

public class PendingJobMapper {

    public static PendingJob toDomain(PanachePendingJobEntity entity) {
        return new PendingJob(
                entity.getId(),
                entity.getProfileId(),
                entity.getEmailReceivedId(),
                entity.getSource(),
                new JobDetail(entity.getJobLink(), entity.getJobTitle(), entity.getJobDescription(),
                        new CompanyDetails(entity.getCompanyName(), entity.getCompanyDescription())),
                entity.isPrioritized(),
                entity.getAttempts(),
                entity.getCreatedAt()
        );
    }

    public static PanachePendingJobEntity toPanachePendingJob(PendingJob pendingJob) {
        JobDetail job = pendingJob.job();
        CompanyDetails company = job.companyDetails();
        return new PanachePendingJobEntity(
                pendingJob.id(),
                pendingJob.profileId(),
                pendingJob.emailReceivedId(),
                pendingJob.source(),
                job.link(),
                job.title(),
                company == null ? null : company.name(),
                company == null ? null : company.description(),
                job.description(),
                pendingJob.prioritized(),
                pendingJob.attempts(),
                pendingJob.createdAt()
        );
    }
}
