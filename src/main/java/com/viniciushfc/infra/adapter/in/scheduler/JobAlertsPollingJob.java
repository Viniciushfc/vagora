package com.viniciushfc.infra.adapter.in.scheduler;

import com.viniciushfc.domain.usecases.contracts.PollJobAlertsUseCase;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobAlertsPollingJob {

    private final PollJobAlertsUseCase pollJobAlertsUseCase;

    public JobAlertsPollingJob(PollJobAlertsUseCase pollJobAlertsUseCase) {
        this.pollJobAlertsUseCase = pollJobAlertsUseCase;
    }

    // SKIP: o processamento de um alerta pode levar minutos (scraping), não sobrepõe execuções
    @Scheduled(every = "{vagora.poll.every}", delayed = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            pollJobAlertsUseCase.execute();
        } catch (Exception e) {
            Log.warn("Falha ao buscar alertas de vaga", e);
        }
    }
}
