package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.database.panache.mapper.EmailReceivedMapper;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheEmailReceivedRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import static com.viniciushfc.infra.adapter.out.database.panache.mapper.EmailReceivedMapper.toPanacheEmailReceived;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PanacheEmailReceivedRepositoryImpl implements EmailReceivedRepository {

    @Inject
    PanacheEmailReceivedRepository panacheEmailReceivedRepository;

    @Inject
    ObjectMapper mapper;

    @Override
    @Transactional
    public void save(EmailReceived emailReceived) {
        panacheEmailReceivedRepository.persist(toPanacheEmailReceived(emailReceived));
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
        entity.setScrapedJobs(null);
    }

    @Override
    @Transactional
    public List<JobDetail> findScrapedJobs(UUID id) {
        var entity = panacheEmailReceivedRepository.findById(id);
        return Utils.isNull(entity) ? List.of() : scrapedJobs(entity.getScrapedJobs());
    }

    @Override
    @Transactional
    public void addScrapedJob(UUID id, JobDetail job) {
        var entity = panacheEmailReceivedRepository.findById(id);
        if (Utils.isNull(entity)) {
            return;
        }
        List<JobDetail> jobs = new ArrayList<>(scrapedJobs(entity.getScrapedJobs()));
        jobs.add(job);
        try {
            entity.setScrapedJobs(mapper.writeValueAsString(jobs));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<JobDetail> scrapedJobs(String json) {
        if (Utils.isNull(json)) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
