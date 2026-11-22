package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.JobMatch;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public interface JobMatchRepository {

    void saveAll(List<JobMatch> jobMatches);

    List<JobMatch> findByProfileId(UUID profileId);

    boolean existsByProfileIdAndJobLink(UUID profileId, String jobLink);

    JobMatch findById(UUID id);

    void updateCurriculum(UUID id, String curriculoAts);

    void updateAppliedAt(UUID id, LocalDateTime appliedAt);
}
