package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.UserAccountMapper;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheUserAccountEntity;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheUserAccountRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.find;

@ApplicationScoped
public class PanacheUserAccountRepositoryImpl implements UserAccountRepository {

    @Inject
    PanacheUserAccountRepository panacheUserAccountRepository;

    @Override
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        var entity = UserAccountMapper.toPanacheUserAccount(userAccount);
        panacheUserAccountRepository.persist(entity);
        return UserAccountMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void delete(UUID idUser) {
        panacheUserAccountRepository.deleteById(idUser);
    }

    @Override
    @Transactional
    public UserAccount update(UUID id, UserAccount userAccount) {
        var entityPersisted = panacheUserAccountRepository.findById(id);

        if (Utils.isNull(entityPersisted)) {
            return null;
        }

        var entityUpdated = UserAccountMapper.mergeForUpdate(entityPersisted, userAccount);
        panacheUserAccountRepository.persist(entityUpdated);

        return UserAccountMapper.toDomain(entityUpdated);
    }

    @Override
    @Transactional
    public UserAccount findById(UUID id) {
        return UserAccountMapper.toDomain(panacheUserAccountRepository.findById(id));
    }

    @Override
    @Transactional
    public UserAccount findByLinkedUsername(String username) {
        return UserAccountMapper.toDomain(
                panacheUserAccountRepository
                        .find("select u from PanacheUserAccountEntity u " +
                                        "join u.linkedAccounts l " +
                                        "where l.username = :username",
                                Map.of("username", username))
                        .firstResult());
    }
}
