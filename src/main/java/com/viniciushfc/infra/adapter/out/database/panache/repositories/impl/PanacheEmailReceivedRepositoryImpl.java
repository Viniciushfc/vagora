package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.EmailReceivedMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheEmailReceivedRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static com.viniciushfc.infra.adapter.out.database.panache.mapper.EmailReceivedMapper.toPanacheEmailReceived;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanacheEmailReceivedRepositoryImpl implements EmailReceivedRepository {

    @Inject
    PanacheEmailReceivedRepository panacheEmailReceivedRepository;

    @Override
    @Transactional
    public void save(EmailReceived emailReceived) {
        panacheEmailReceivedRepository.persist(toPanacheEmailReceived(emailReceived));
    }

    @Override
    public void delete(UUID uuidEmail) {

    }

    @Override
    public List<EmailReceived> findAll() {
        return List.of();
    }

    @Override
    public EmailReceived findById(UUID id) {
        return null;
    }

    @Override
    @Transactional
    public boolean existsEmailByMessageId(String messageID) {
        return panacheEmailReceivedRepository.count("messageId", messageID) > 0;
    }

    @Override
    @Transactional
    public EmailReceived findByMessageId(String messageId) {
        var entity = panacheEmailReceivedRepository.find("messageId", messageId).firstResult();
        return Utils.isNull(entity) ? null : EmailReceivedMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void markSuccessProcessed(UUID id) {
        var entity = panacheEmailReceivedRepository.findById(id);

        if (Utils.isNull(entity)) {
            return;
        }

        entity.setSuccessProcessed(true);
    }
}
