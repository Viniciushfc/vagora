package com.viniciushfc.infra.adapter.in.scheduler;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
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

    @Scheduled(every = "{vagora.poll.every}", delayed = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        for (AlertSource source : AlertSource.values()) {
            try {
                pollJobAlertsUseCase.execute(source);
            } catch (ExternalServiceException e) {
                Log.warnf("Falha ao buscar alertas de vaga do %s: %s", source.label, e.getMessage());
            } catch (Exception e) {
                Log.warn("Falha ao buscar alertas de vaga do " + source.label, e);
            }
        }
    }
}
