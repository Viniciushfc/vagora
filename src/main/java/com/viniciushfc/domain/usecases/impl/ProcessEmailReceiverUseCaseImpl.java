package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    // formato do link salvo nas vagas analisadas (o mesmo que o scraper devolve)
    static final String JOB_URL = "https://www.linkedin.com/jobs/view/%s/";

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
    public boolean execute(Profile profile, EmailReceived received) {
        if (!received.isAlertJob()) {
            return false;
        }

        EmailReceived existing = emailReceivedRepository.findByMessageId(received.messageId());
        if (existing != null && existing.successProcessed()) {
            return false;
        }

        LinkedAccount linkedinAccount = profile.findLinkedAccount(ExternalProvider.LINKEDIN);
        if (Utils.isNull(linkedinAccount)) {
            throw new BusinessException("Conecte uma conta do LinkedIn em Contas.", ErrorCode.LINKEDIN_NOT_CONNECTED);
        }

        if (Utils.isNull(profile.geminiApiKey()) || profile.geminiApiKey().isBlank()) {
            throw new BusinessException("Configure a chave do Gemini no Perfil.", ErrorCode.GEMINI_KEY_MISSING);
        }

        // o banco guarda só id e status; o conteúdo vem sempre do email recebido agora
        UUID emailId = existing != null ? existing.id() : received.id();
        if (existing == null) {
            emailReceivedRepository.save(received);
        }

        // vaga já analisada não é aberta de novo: o ID vem no link do email
        Set<String> links = new LinkedHashSet<>();
        extractLinksJobs(received.email().body()).forEach((jobId, link) -> {
            if (!jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), JOB_URL.formatted(jobId))) {
                links.add(link);
            }
        });
        if (links.isEmpty()) {
            emailReceivedRepository.markSuccessProcessed(emailId);
            return false;
        }

        List<JobDetail> jobDetailList = jobDetailsGateway.extractJobsDetails(links, linkedinAccount, profile.isBrowserVisible());
        if (jobDetailList.isEmpty()) {
            emailReceivedRepository.markSuccessProcessed(emailId);
            return true;
        }

        String userCurriculumText = pdfGateway.extractTextPdf(profile.curriculum());
        analyze(profile, emailId, jobDetailList, userCurriculumText);
        emailReceivedRepository.markSuccessProcessed(emailId);
        return true;
    }

    // uma vaga por vez, registrando o progresso para a página de acompanhamento
    private List<JobMatch> analyze(Profile profile, UUID emailId, List<JobDetail> jobs, String curriculum) {
        List<JobMatch> jobMatches = new ArrayList<>();
        try {
            for (int i = 0; i < jobs.size(); i++) {
                JobDetail job = jobs.get(i);
                int index = i + 1;
                activity(a -> a.analyzing(index, jobs.size(), job.title()));
                CvMatchResult result = analyzeJobsMatchCvGateway.analyze(job, curriculum, profile.geminiApiKey());
                JobMatch jobMatch = JobMatch.create(profile.id(), emailId, job, result);
                // salva na hora: se a próxima falhar, esta não é aberta de novo no LinkedIn
                jobMatchRepository.saveAll(List.of(jobMatch));
                jobMatches.add(jobMatch);
            }
        } catch (RuntimeException e) {
            String message = e instanceof DomainException ? e.getMessage() : "Erro inesperado na análise do Gemini.";
            activity(a -> a.failed(message, a.screenshot()));
            throw e;
        }
        long matches = jobMatches.stream().filter(JobMatch::match).count();
        activity(a -> a.analysisFinished((int) matches, jobs.size()));
        return jobMatches;
    }

    private void activity(UnaryOperator<ScraperActivity> change) {
        scraperActivityRepository.save(change.apply(scraperActivityRepository.get()));
    }

    /** ID da vaga -> link do email, na ordem em que aparecem. */
    private Map<String, String> extractLinksJobs(String body) {
        Map<String, String> linksById = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile(
                "https://www\\.linkedin\\.com/comm/jobs/view/(\\d+)/?(?:\\?[^\\s\"'<>]*)?").matcher(body);
        while (matcher.find()) {
            linksById.putIfAbsent(matcher.group(1), matcher.group().replace("&amp;", "&"));
        }

        return linksById;
    }
}
