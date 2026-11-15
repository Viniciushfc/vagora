package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.ProfileMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanacheProfileRepositoryImpl implements ProfileRepository {

    @Inject
    PanacheProfileRepository panacheProfileRepository;

    @Override
    @Transactional
    public Profile save(Profile profile) {
        var entity = ProfileMapper.toPanacheProfile(profile);
        panacheProfileRepository.persist(entity);
        return ProfileMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        panacheProfileRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Profile update(UUID id, Profile profile) {
        var entityPersisted = panacheProfileRepository.findById(id);

        if (Utils.isNull(entityPersisted)) {
            return null;
        }

        var entityUpdated = ProfileMapper.mergeForUpdate(entityPersisted, profile);
        panacheProfileRepository.persist(entityUpdated);

        return ProfileMapper.toDomain(entityUpdated);
    }

    @Override
    @Transactional
    public Profile updateLinkedAccounts(UUID id, List<LinkedAccount> linkedAccounts) {
        var entity = panacheProfileRepository.findById(id);
        if (Utils.isNull(entity)) {
            return null;
        }
        entity.setLinkedAccounts(LinkedAccountMapper.toPanacheLinkedAccountEmbeddable(linkedAccounts));
        return ProfileMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public Profile find() {
        return ProfileMapper.toDomain(panacheProfileRepository.findAll().firstResult());
    }
}
