package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.JobMatchMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheJobMatchRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@ApplicationScoped
public class PanacheJobMatchRepositoryImpl implements JobMatchRepository {

    @Inject
    PanacheJobMatchRepository panacheJobMatchRepository;

    @Override
    @Transactional
    public void saveAll(List<JobMatch> jobMatches) {
        panacheJobMatchRepository.persist(jobMatches.stream().map(JobMatchMapper::toPanacheJobMatch));
    }

    @Override
    @Transactional
    public List<JobMatch> findByProfileId(UUID profileId) {
        return panacheJobMatchRepository.list("profileId", profileId)
                .stream()
                .map(JobMatchMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean existsByProfileIdAndJobLink(UUID profileId, String jobLink) {
        return panacheJobMatchRepository
                .count("profileId = :profileId and jobLink = :jobLink",
                        Map.of("profileId", profileId, "jobLink", jobLink)) > 0;
    }

    @Override
    @Transactional
    public JobMatch findById(UUID id) {
        var entity = panacheJobMatchRepository.findById(id);
        return entity == null ? null : JobMatchMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void updateCurriculum(UUID id, String curriculoAts) {
        var entity = panacheJobMatchRepository.findById(id);
        if (entity != null) {
            entity.setCurriculoAts(curriculoAts);
        }
    }

    @Override
    @Transactional
    public void updateAnalysis(UUID id, CvMatchResult result) {
        var entity = panacheJobMatchRepository.findById(id);
        if (entity != null) {
            entity.setMatch(result.match());
            entity.setScore(result.score());
            entity.setMotivos(result.motivos());
        }
    }

    @Override
    @Transactional
    public void updateAppliedAt(UUID id, LocalDateTime appliedAt) {
        var entity = panacheJobMatchRepository.findById(id);
        if (entity != null) {
            entity.setAppliedAt(appliedAt);
        }
    }
}
