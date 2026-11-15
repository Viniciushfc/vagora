package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.JobMatch;

import java.util.List;
import java.util.UUID;

public interface JobMatchRepository {

    void saveAll(List<JobMatch> jobMatches);

    List<JobMatch> findByUserAccountId(UUID userAccountId);

    boolean existsByUserAccountIdAndJobLink(UUID userAccountId, String jobLink);
}
