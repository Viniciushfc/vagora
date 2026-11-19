package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.*;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    private final EmailReceivedRepository emailReceivedRepository;
    private final JobDetailsGateway jobDetailsGateway;
    private final JobMatchRepository jobMatchRepository;
    private final PendingJobRepository pendingJobRepository;

    public ProcessEmailReceiverUseCaseImpl(EmailReceivedRepository emailReceivedRepository, JobDetailsGateway jobDetailsGateway,
                                           JobMatchRepository jobMatchRepository, PendingJobRepository pendingJobRepository) {
        this.emailReceivedRepository = emailReceivedRepository;
        this.jobDetailsGateway = jobDetailsGateway;
        this.jobMatchRepository = jobMatchRepository;
        this.pendingJobRepository = pendingJobRepository;
    }

    @Override
    public boolean execute(Profile profile, AlertSource source, List<EmailReceived> received) {
        List<EmailReceived> alerts = received.stream().filter(alert -> alert.isFrom(source)).toList();
        if (alerts.isEmpty()) {
            return false;
        }

        LinkedAccount linkedinAccount = profile.findLinkedAccount(ExternalProvider.LINKEDIN);
        if (source == AlertSource.LINKEDIN && linkedinAccount == null) {
            throw new BusinessException("Conecte uma conta do LinkedIn em Configurações.", ErrorCode.LINKEDIN_NOT_CONNECTED);
        }

        List<UUID> emailIds = new ArrayList<>();
        Map<String, UUID> emailOf = new HashMap<>();
        Map<String, String> toScrape = new LinkedHashMap<>();
        for (EmailReceived alert : alerts) {
            EmailReceived existing = emailReceivedRepository.findByMessageId(alert.messageId());
            if (existing != null && existing.successProcessed()) {
                continue;
            }
            UUID emailId = existing != null ? existing.id() : alert.id();
            if (existing == null) {
                emailReceivedRepository.save(alert);
            }
            emailIds.add(emailId);

            for (JobDetail job : emailReceivedRepository.findScrapedJobs(emailId)) {
                if (emailOf.putIfAbsent(job.link(), emailId) == null && !known(profile, job.link())) {
                    pendingJobRepository.save(PendingJob.create(profile.id(), emailId, source, job));
                }
            }
            source.jobLinks(alert.email().body()).forEach((jobUrl, link) -> {
                if (emailOf.putIfAbsent(jobUrl, emailId) == null && !known(profile, jobUrl)) {
                    toScrape.put(link, jobUrl);
                }
            });
        }
        if (emailIds.isEmpty()) {
            return false;
        }

        if (!toScrape.isEmpty()) {
            jobDetailsGateway.extractJobsDetails(source, toScrape.keySet(), linkedinAccount, profile.isBrowserVisible(), read -> {
                String jobUrl = toScrape.get(read.link());
                JobDetail job = new JobDetail(jobUrl, read.title(), read.description(), read.companyDetails());
                pendingJobRepository.save(PendingJob.create(profile.id(), emailOf.get(jobUrl), source, job));
            });
        }
        emailIds.forEach(emailReceivedRepository::markSuccessProcessed);
        return !toScrape.isEmpty();
    }

    private boolean known(Profile profile, String jobUrl) {
        return jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), jobUrl)
                || pendingJobRepository.existsByProfileIdAndLink(profile.id(), jobUrl);
    }
}
