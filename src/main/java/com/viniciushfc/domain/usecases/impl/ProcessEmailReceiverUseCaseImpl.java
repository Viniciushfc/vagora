package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.gateways.*;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    private final EmailReceivedRepository emailReceivedRepository;
    private final JobDetailsGateway jobDetailsGateway;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final UserAccountRepository userAccountRepository;
    private final PdfGateway pdfGateway;
    private final JobMatchRepository jobMatchRepository;

    public ProcessEmailReceiverUseCaseImpl(EmailReceivedRepository emailReceivedRepository, JobDetailsGateway jobDetailsGateway, AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, UserAccountRepository userAccountRepository, PdfGateway pdfGateway, JobMatchRepository jobMatchRepository) {
        this.emailReceivedRepository = emailReceivedRepository;
        this.jobDetailsGateway = jobDetailsGateway;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.userAccountRepository = userAccountRepository;
        this.pdfGateway = pdfGateway;
        this.jobMatchRepository = jobMatchRepository;
    }

    @Override
    public void execute(EmailReceived received) throws IOException {
        if (!received.isAlertJob()) {
            return;
        }

        EmailReceived existing = emailReceivedRepository.findByMessageId(received.messageId());
        if (existing != null && existing.successProcessed()) {
            return;
        }

        UserAccount userAccount = userAccountRepository.findByLinkedUsername(received.email().to());
        if (userAccount == null) {
            throw new IllegalStateException("Nenhum usuário vinculado ao email: " + received.email().to());
        }

        EmailReceived emailReceived = existing;
        if (emailReceived == null) {
            emailReceived = received;
            emailReceivedRepository.save(emailReceived);
        }

        Set<String> links = extractLinksJobs(emailReceived.email().body());
        List<JobDetail> jobDetails = jobDetailsGateway.extractJobsDetails(links);

        List<JobDetail> jobDetailList = jobDetails.stream()
                .filter(job -> !jobMatchRepository.existsByUserAccountIdAndJobLink(userAccount.id(), job.link()))
                .toList();

        if (jobDetailList.isEmpty()) {
            emailReceivedRepository.markSuccessProcessed(emailReceived.id());
            return;
        }

        String userCurriculumText = pdfGateway.extractTextPdf(userAccount.curriculum());
        List<CvMatchResult> cvMatchResults = analyzeJobsMatchCvGateway.executeAnalysis(jobDetailList, userCurriculumText);

        if (cvMatchResults.size() != jobDetailList.size()) {
            throw new IllegalStateException("Quantidade de resultados (" + cvMatchResults.size()
                    + ") diferente da quantidade de vagas (" + jobDetailList.size() + ")");
        }

        List<JobMatch> jobMatches = new ArrayList<>();
        for (int i = 0; i < jobDetailList.size(); i++) {
            jobMatches.add(JobMatch.create(userAccount.id(), emailReceived.id(), jobDetailList.get(i), cvMatchResults.get(i)));
        }

        jobMatchRepository.saveAll(jobMatches);
        emailReceivedRepository.markSuccessProcessed(emailReceived.id());
    }

    private Set<String> extractLinksJobs(String body) {
        Map<String, String> linksById = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile(
                "https://www\\.linkedin\\.com/comm/jobs/view/(\\d+)/?(?:\\?[^\\s\"'<>]*)?").matcher(body);
        while (matcher.find()) {
            linksById.putIfAbsent(matcher.group(1), matcher.group().replace("&amp;", "&"));
        }

        return new LinkedHashSet<>(linksById.values());
    }
}
