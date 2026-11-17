package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;
import java.util.UUID;

public interface EmailReceivedRepository {
    void save(EmailReceived emailReceived);

    EmailReceived findByMessageId(String messageId);

    void markSuccessProcessed(UUID id);

    List<JobDetail> findScrapedJobs(UUID id);

    void addScrapedJob(UUID id, JobDetail job);
}
