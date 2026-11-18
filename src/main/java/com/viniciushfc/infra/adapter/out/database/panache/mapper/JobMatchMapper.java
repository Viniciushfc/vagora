package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheJobMatchEntity;

import java.util.List;

public class JobMatchMapper {

    public static JobMatch toDomain(PanacheJobMatchEntity entity) {
        return new JobMatch(
                entity.getId(),
                entity.getProfileId(),
                entity.getEmailReceivedId(),
                entity.getJobLink(),
                entity.getJobTitle(),
                entity.getCompanyName(),
                entity.isMatch(),
                entity.getScore(),
                List.copyOf(entity.getMotivos()),
                entity.getCurriculoAts(),
                entity.getCreatedAt(),
                entity.getJobDescription()
        );
    }

    public static PanacheJobMatchEntity toPanacheJobMatch(JobMatch jobMatch) {
        return new PanacheJobMatchEntity(
                jobMatch.id(),
                jobMatch.profileId(),
                jobMatch.emailReceivedId(),
                jobMatch.jobLink(),
                jobMatch.jobTitle(),
                jobMatch.companyName(),
                jobMatch.match(),
                jobMatch.score(),
                jobMatch.motivos(),
                jobMatch.curriculoAts(),
                jobMatch.createdAt(),
                jobMatch.jobDescription()
        );
    }
}
