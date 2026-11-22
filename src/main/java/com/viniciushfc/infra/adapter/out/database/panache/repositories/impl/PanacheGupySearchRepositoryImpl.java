package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.GupySearchMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheGupySearchRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanacheGupySearchRepositoryImpl implements GupySearchRepository {

    @Inject
    PanacheGupySearchRepository panacheGupySearchRepository;

    @Override
    @Transactional
    public List<GupySearch> findByProfileId(UUID profileId) {
        return panacheGupySearchRepository.list("profileId", Sort.ascending("jobName"), profileId).stream()
                .map(GupySearchMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public GupySearch findById(UUID id) {
        var entity = panacheGupySearchRepository.findById(id);
        return entity == null ? null : GupySearchMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void save(GupySearch search) {
        panacheGupySearchRepository.getEntityManager().merge(GupySearchMapper.toEntity(search));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        panacheGupySearchRepository.deleteById(id);
    }
}
