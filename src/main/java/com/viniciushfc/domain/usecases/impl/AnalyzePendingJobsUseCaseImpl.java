package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.usecases.contracts.AnalyzePendingJobsUseCase;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.ProcessingStatus;
import com.viniciushfc.domain.vo.ScraperActivity;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@UseCase
public class AnalyzePendingJobsUseCaseImpl implements AnalyzePendingJobsUseCase {

    public static final int BATCH_SIZE = 10;
    public static final int MAX_ATTEMPTS = 3;

    private static final System.Logger LOG = System.getLogger(AnalyzePendingJobsUseCaseImpl.class.getName());

    private final ProfileRepository profileRepository;
    private final PendingJobRepository pendingJobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;
    private final ProcessingStatusRepository statusRepository;
    private final ScraperActivityRepository scraperActivityRepository;

    public AnalyzePendingJobsUseCaseImpl(ProfileRepository profileRepository,
                                         PendingJobRepository pendingJobRepository,
                                         JobMatchRepository jobMatchRepository,
                                         AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway,
                                         PdfGateway pdfGateway,
                                         ProcessingStatusRepository statusRepository,
                                         ScraperActivityRepository scraperActivityRepository) {
        this.profileRepository = profileRepository;
        this.pendingJobRepository = pendingJobRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
        this.statusRepository = statusRepository;
        this.scraperActivityRepository = scraperActivityRepository;
    }

    @Override
    public void execute() {
        Profile profile = profileRepository.find();
        if (profile == null || !profile.isMonitoringEnabled() || profile.llmProviders().isEmpty()) {
            return;
        }
        ProcessingStatus status = statusRepository.findAnalysis(profile.id());
        if (status.isWaiting(LocalDateTime.now())) {
            return;
        }

        List<PendingJob> batch = pendingJobRepository.nextBatch(profile.id(), MAX_ATTEMPTS, BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        PendingJob first = batch.getFirst();
        if (scraperActivityRepository.get(first.source()).running()) {
            return;
        }

        List<PendingJob> jobs = first.attempts() > 0
                ? List.of(first)
                : fitToProviders(sameSource(batch, first.source()), profile);

        List<PendingJob> analyzed = jobs.stream()
                .filter(job -> jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), job.link()))
                .toList();
        if (!analyzed.isEmpty()) {
            pendingJobRepository.delete(ids(analyzed));
            jobs = jobs.stream().filter(job -> !analyzed.contains(job)).toList();
            if (jobs.isEmpty()) {
                return;
            }
        }

        String label = jobs.size() + (jobs.size() == 1 ? " vaga" : " vagas");
        statusRepository.saveAnalysis(profile.id(), status.analysing(label, 0));
        try {
            analyze(profile, first.source(), jobs);
            ProcessingStatus succeeded = statusRepository.findAnalysis(profile.id()).succeeded().checked();
            statusRepository.saveAnalysis(profile.id(), succeeded);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Falha ao analisar " + jobs.size() + " vagas pendentes", e);
            ProcessingStatus failed = statusRepository.findAnalysis(profile.id()).failed(e).checked();
            if (isInvalidResponse(e)) {
                pendingJobRepository.registerFailure(ids(jobs));
            } else {
                failed = failed.waitUntil(LocalDateTime.now().plus(waitAfter(e)));
            }
            statusRepository.saveAnalysis(profile.id(), failed);
        }
    }

    private static List<PendingJob> sameSource(List<PendingJob> batch, AlertSource source) {
        return batch.stream()
                .filter(job -> job.source() == source)
                .toList();
    }

    private static List<PendingJob> fitToProviders(List<PendingJob> jobs, Profile profile) {
        int limit = profile.llmProviders().stream()
                .mapToInt(provider -> provider.maxJobChars)
                .min()
                .orElse(Integer.MAX_VALUE);

        List<PendingJob> fit = new ArrayList<>();
        long chars = 0;
        for (PendingJob job : jobs) {
            chars += textLength(job.job());
            if (!fit.isEmpty() && chars > limit) {
                break;
            }
            fit.add(job);
        }
        return fit;
    }

    private static int textLength(JobDetail job) {
        int company = job.companyDetails() == null ? 0 : length(job.companyDetails().description());
        return length(job.description()) + company;
    }

    private static int length(String text) {
        return text == null ? 0 : text.length();
    }

    private static List<UUID> ids(List<PendingJob> jobs) {
        return jobs.stream()
                .map(PendingJob::id)
                .toList();
    }

    private static boolean isInvalidResponse(Exception e) {
        return e instanceof DomainException domain && domain.code() == ErrorCode.LLM_INVALID_RESPONSE;
    }

    private static Duration waitAfter(Exception e) {
        if (e instanceof RateLimitedException limited
                && limited.retryAfter().compareTo(PollJobAlertsUseCaseImpl.MIN_INTERVAL) > 0) {
            return limited.retryAfter();
        }
        return PollJobAlertsUseCaseImpl.nextInterval();
    }

    private void analyze(Profile profile, AlertSource source, List<PendingJob> jobs) {
        String curriculum = pdfGateway.extractTextPdf(profile.curriculum());
        List<JobMatch> jobMatches = new ArrayList<>();
        try {
            activity(source, a -> a.analyzing(jobs.size()));
            List<JobDetail> details = jobs.stream().map(PendingJob::job).toList();
            List<CvMatchResult> results = analyzeJobsMatchCvGateway.analyze(details, curriculum, profile);
            for (int i = 0; i < jobs.size(); i++) {
                PendingJob job = jobs.get(i);
                jobMatches.add(JobMatch.create(profile.id(), job.emailReceivedId(), job.job(), results.get(i)));
            }
            jobMatchRepository.saveAll(jobMatches);
            pendingJobRepository.delete(ids(jobs));
        } catch (RuntimeException e) {
            String message = e instanceof DomainException ? e.getMessage() : "Erro inesperado na análise.";
            activity(source, a -> a.failed(message, a.screenshot()));
            throw e;
        }

        if (profile.isAutoCurriculum()) {
            adaptCurricula(profile, source, jobs, jobMatches, curriculum);
        }

        long matches = jobMatches.stream().filter(JobMatch::match).count();
        activity(source, a -> a.analysisFinished((int) matches, jobs.size()));
    }

    private void adaptCurricula(Profile profile,
                                AlertSource source,
                                List<PendingJob> jobs,
                                List<JobMatch> jobMatches,
                                String curriculum) {
        for (int i = 0; i < jobMatches.size(); i++) {
            JobMatch jobMatch = jobMatches.get(i);
            if (!jobMatch.match()) {
                continue;
            }
            activity(source, a -> a.adapting(jobMatch.jobTitle()));
            try {
                String adapted = analyzeJobsMatchCvGateway.adaptCurriculum(jobs.get(i).job(), curriculum, profile);
                jobMatchRepository.updateCurriculum(jobMatch.id(), adapted);
            } catch (RuntimeException e) {
                String message = e instanceof DomainException ? e.getMessage() : "Erro inesperado na IA.";
                String event = "Currículo não gerado (" + message + "). Use \"Gerar currículo\" na vaga.";
                activity(source, a -> a.event(event, true));
                return;
            }
        }
    }

    private void activity(AlertSource source, UnaryOperator<ScraperActivity> change) {
        scraperActivityRepository.save(source, change.apply(scraperActivityRepository.get(source)));
    }
}
