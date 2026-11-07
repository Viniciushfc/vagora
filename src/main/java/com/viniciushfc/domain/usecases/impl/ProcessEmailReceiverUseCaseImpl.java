package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.gateway.EmailGateway;
import com.viniciushfc.domain.gateway.JobDetailsGateway;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    private final EmailGateway emailGateway;
    private final JobDetailsGateway jobDetailsGateway;

    public ProcessEmailReceiverUseCaseImpl(EmailGateway emailGateway, JobDetailsGateway jobDetailsGateway) {
        this.emailGateway = emailGateway;
        this.jobDetailsGateway = jobDetailsGateway;
    }

    @Override
    public void execute(EmailReceived emailReceived) {
        if (!emailReceived.isAlertJob()) {
            return;
        }

        Set<String> links = extractLinksJobs(emailReceived.email().body());

        List<JobDetail> jobDetailList = jobDetailsGateway.extractJobsDetails(links);

        emailGateway.save(emailReceived);
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
