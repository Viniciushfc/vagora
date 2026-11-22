package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.GupySearch;

import java.util.List;
import java.util.UUID;

public interface GupySearchRepository {
    List<GupySearch> findByProfileId(UUID profileId);

    GupySearch findById(UUID id);

    void save(GupySearch search);

    void delete(UUID id);
}
