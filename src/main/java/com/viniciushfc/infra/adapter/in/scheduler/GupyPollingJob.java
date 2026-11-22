package com.viniciushfc.infra.adapter.in.scheduler;

import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.usecases.contracts.PollGupySearchesUseCase;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GupyPollingJob {

    private final PollGupySearchesUseCase pollGupySearchesUseCase;

    public GupyPollingJob(PollGupySearchesUseCase pollGupySearchesUseCase) {
        this.pollGupySearchesUseCase = pollGupySearchesUseCase;
    }

    @Scheduled(every = "{vagora.gupy.every}", delayed = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        try {
            pollGupySearchesUseCase.execute();
        } catch (ExternalServiceException e) {
            Log.warnf("Falha ao buscar vagas na Gupy: %s", e.getMessage());
        } catch (Exception e) {
            Log.warn("Falha ao buscar vagas na Gupy", e);
        }
    }
}
