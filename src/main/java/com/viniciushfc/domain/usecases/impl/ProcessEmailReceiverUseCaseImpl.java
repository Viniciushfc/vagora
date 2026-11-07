package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.gateway.EmailGateway;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UseCase
public class ProcessEmailReceiverUseCaseImpl implements ProcessEmailReceiverUseCase {

    private final EmailGateway emailGateway;

    public ProcessEmailReceiverUseCaseImpl(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    @Override
    public void execute(EmailReceived emailReceived) {
        if (!emailReceived.isAlertJob()) {
            return;
        }

        Set<String> links = extractLinksJobs(emailReceived.email().body());

        // CHAMAR UMA FERRAMENTA DE AUTOMAÇÃO DE NAVEGADOR EX: Selenium ou Playwright;
        // OBJETO ENTRAR NO LINK E EXTRAIR AS SESSÕES NECESSARIAS TITULO DA VAGA,SOBRE A VAGA, SOBRE EMPRESA;

        emailGateway.save(emailReceived);
    }

    private Set<String> extractLinksJobs(String body) {
        Matcher matcher = Pattern.compile("https://www.linkedin.com/comm/jobs/view/[\\d+]+").matcher(body);
        Set<String> links = new HashSet<>();
        while (matcher.find()) {
            links.add(matcher.group());
        }

        return links;
    }
}
