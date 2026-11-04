package com.viniciushfc.infra.adapter.in.camel.mail.router;

import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.infra.adapter.in.camel.mail.mapper.EmailMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class EmailPollingRoute extends RouteBuilder {

    @Inject
    ProcessEmailReceiverUseCase processEmailReceiverUseCase;

    @Inject
    EmailMapper mapper;

    @Override
    public void configure() {

        // Tratamento de erro: tenta 3 vezes e depois só loga
        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .handled(true)
                .log("Falha ao processar e-mail '${header.Subject}': ${exception.message}");

        from("imaps://{{mail.host}}:{{mail.port}}"
                + "?username={{mail.username}}"
                + "&password=RAW({{mail.password}})"
                + "&folderName={{mail.folder}}"
                + "&unseen=true"            // só busca não lidos
                + "&delete=false"           // não apaga do servidor
                + "&peek=true"              // só marca como lido se processar com sucesso
                + "&fetchSize=10"           // no máximo 10 por polling
                + "&initialDelay={{mail.poll.initial-delay}}"
                + "&delay={{mail.poll.delay}}")
                .routeId("email-polling")
                .log("Novo e-mail de ${header.From}: ${header.Subject}")
                .process(exchange -> processEmailReceiverUseCase.execute(mapper.toDomain(exchange)));
    }
}