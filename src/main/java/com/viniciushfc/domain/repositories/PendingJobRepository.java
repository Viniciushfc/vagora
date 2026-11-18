package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.PendingJob;

import java.util.List;
import java.util.UUID;

public interface PendingJobRepository {

    void save(PendingJob pendingJob);

    boolean existsByProfileIdAndLink(UUID profileId, String link);

    List<PendingJob> findByProfileId(UUID profileId);

    List<PendingJob> nextBatch(UUID profileId, int maxAttempts, int limit);

    PendingJob findById(UUID id);

    void prioritize(UUID id);

    void registerFailure(List<UUID> ids);

    void delete(List<UUID> ids);
}
