package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.gateways.*;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
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

    public ProcessEmailReceiverUseCaseImpl(EmailReceivedRepository emailReceivedRepository, JobDetailsGateway jobDetailsGateway, AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, UserAccountRepository userAccountRepository, PdfGateway pdfGateway) {
        this.emailReceivedRepository = emailReceivedRepository;
        this.jobDetailsGateway = jobDetailsGateway;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.userAccountRepository = userAccountRepository;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public void execute(EmailReceived emailReceived) throws IOException {
        if (!emailReceived.isAlertJob() || emailReceivedRepository.existsEmailByMessageId(emailReceived.messageId())) {
            return;
        }

        emailReceivedRepository.save(emailReceived);

        Set<String> links = extractLinksJobs(emailReceived.email().body());
        List<JobDetail> jobDetailList = jobDetailsGateway.extractJobsDetails(links);
        UserAccount userAccount = userAccountRepository.findByLinkedUsername(emailReceived.email().to());

        String userCurriculumText = pdfGateway.extractTextPdf(userAccount.curriculum());
        List<CvMatchResult> cvMatchResults = analyzeJobsMatchCvGateway.executeAnalysis(jobDetailList, userCurriculumText);

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
