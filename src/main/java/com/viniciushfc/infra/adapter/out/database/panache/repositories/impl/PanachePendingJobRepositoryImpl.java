package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.PendingJobMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanachePendingJobRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanachePendingJobRepositoryImpl implements PendingJobRepository {

    private static final Sort QUEUE_ORDER = Sort.descending("prioritized").and("attempts").and("createdAt");

    @Inject
    PanachePendingJobRepository panachePendingJobRepository;

    @Override
    @Transactional
    public void save(PendingJob pendingJob) {
        panachePendingJobRepository.persist(PendingJobMapper.toPanachePendingJob(pendingJob));
    }

    @Override
    @Transactional
    public boolean existsByProfileIdAndLink(UUID profileId, String link) {
        return panachePendingJobRepository.count("profileId = :profileId and jobLink = :link",
                Parameters.with("profileId", profileId).and("link", link)) > 0;
    }

    @Override
    @Transactional
    public List<PendingJob> findByProfileId(UUID profileId) {
        return panachePendingJobRepository.list("profileId", QUEUE_ORDER, profileId).stream()
                .map(PendingJobMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<PendingJob> nextBatch(UUID profileId, int maxAttempts, int limit) {
        return panachePendingJobRepository
                .find("profileId = :profileId and (attempts < :maxAttempts or prioritized = true)", QUEUE_ORDER,
                        Parameters.with("profileId", profileId).and("maxAttempts", maxAttempts))
                .page(0, limit)
                .stream()
                .map(PendingJobMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public PendingJob findById(UUID id) {
        var entity = panachePendingJobRepository.findById(id);
        return entity == null ? null : PendingJobMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void prioritize(UUID id) {
        var entity = panachePendingJobRepository.findById(id);
        if (entity != null) {
            entity.setPrioritized(true);
        }
    }

    @Override
    @Transactional
    public void registerFailure(List<UUID> ids) {
        for (UUID id : ids) {
            var entity = panachePendingJobRepository.findById(id);
            if (entity != null) {
                entity.setAttempts(entity.getAttempts() + 1);
                entity.setPrioritized(false);
            }
        }
    }

    @Override
    @Transactional
    public void delete(List<UUID> ids) {
        if (!ids.isEmpty()) {
            panachePendingJobRepository.delete("id in ?1", ids);
        }
    }
}
