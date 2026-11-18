package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.ScraperActivity;

import java.util.*;
import java.util.function.UnaryOperator;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    private final EmailReceivedRepository emailReceivedRepository;
    private final JobDetailsGateway jobDetailsGateway;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;
    private final JobMatchRepository jobMatchRepository;
    private final ScraperActivityRepository scraperActivityRepository;

    public ProcessEmailReceiverUseCaseImpl(EmailReceivedRepository emailReceivedRepository, JobDetailsGateway jobDetailsGateway, AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, PdfGateway pdfGateway, JobMatchRepository jobMatchRepository, ScraperActivityRepository scraperActivityRepository) {
        this.emailReceivedRepository = emailReceivedRepository;
        this.jobDetailsGateway = jobDetailsGateway;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
        this.jobMatchRepository = jobMatchRepository;
        this.scraperActivityRepository = scraperActivityRepository;
    }

    @Override
    public boolean execute(Profile profile, AlertSource source, List<EmailReceived> received) {
        List<EmailReceived> alerts = received.stream().filter(alert -> alert.isFrom(source)).toList();
        if (alerts.isEmpty()) {
            return false;
        }

        LinkedAccount linkedinAccount = profile.findLinkedAccount(ExternalProvider.LINKEDIN);
        if (source == AlertSource.LINKEDIN && Utils.isNull(linkedinAccount)) {
            throw new BusinessException("Conecte uma conta do LinkedIn em Contas.", ErrorCode.LINKEDIN_NOT_CONNECTED);
        }

        if (Utils.isNull(profile.geminiApiKey()) || profile.geminiApiKey().isBlank()) {
            throw new BusinessException("Configure a chave do Gemini no Perfil.", ErrorCode.GEMINI_KEY_MISSING);
        }

        List<UUID> emailIds = new ArrayList<>();
        Map<String, UUID> emailOf = new HashMap<>();
        List<JobDetail> jobs = new ArrayList<>();
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
                if (emailOf.putIfAbsent(job.link(), emailId) == null && !analyzed(profile, job.link())) {
                    jobs.add(job);
                }
            }
            source.jobLinks(alert.email().body()).forEach((jobUrl, link) -> {
                if (emailOf.putIfAbsent(jobUrl, emailId) == null && !analyzed(profile, jobUrl)) {
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
                emailReceivedRepository.addScrapedJob(emailOf.get(jobUrl), job);
                jobs.add(job);
            });
        }
        if (!jobs.isEmpty()) {
            analyze(profile, source, emailOf, jobs, pdfGateway.extractTextPdf(profile.curriculum()));
        }
        emailIds.forEach(emailReceivedRepository::markSuccessProcessed);
        return !toScrape.isEmpty();
    }

    private boolean analyzed(Profile profile, String jobUrl) {
        return jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), jobUrl);
    }

    private List<JobMatch> analyze(Profile profile, AlertSource source, Map<String, UUID> emailOf, List<JobDetail> jobs, String curriculum) {
        List<JobMatch> jobMatches = new ArrayList<>();
        try {
            activity(source, a -> a.analyzing(jobs.size()));
            List<CvMatchResult> results = analyzeJobsMatchCvGateway.analyze(jobs, curriculum, profile.geminiApiKey());
            for (int i = 0; i < jobs.size(); i++) {
                JobDetail job = jobs.get(i);
                CvMatchResult result = results.get(i);
                if (result.match()) {
                    activity(source, a -> a.adapting(job.title()));
                    String adapted = analyzeJobsMatchCvGateway.adaptCurriculum(job, curriculum, profile.geminiApiKey());
                    result = new CvMatchResult(true, result.score(), result.motivos(), adapted);
                }
                JobMatch jobMatch = JobMatch.create(profile.id(), emailOf.get(job.link()), job, result);
                jobMatchRepository.saveAll(List.of(jobMatch));
                jobMatches.add(jobMatch);
            }
        } catch (RuntimeException e) {
            String message = e instanceof DomainException ? e.getMessage() : "Erro inesperado na análise do Gemini.";
            activity(source, a -> a.failed(message, a.screenshot()));
            throw e;
        }
        long matches = jobMatches.stream().filter(JobMatch::match).count();
        activity(source, a -> a.analysisFinished((int) matches, jobs.size()));
        return jobMatches;
    }

    private void activity(AlertSource source, UnaryOperator<ScraperActivity> change) {
        scraperActivityRepository.save(source, change.apply(scraperActivityRepository.get(source)));
    }
}
