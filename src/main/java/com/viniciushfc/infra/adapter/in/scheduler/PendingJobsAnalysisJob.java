package com.viniciushfc.infra.adapter.in.scheduler;

import com.viniciushfc.domain.usecases.contracts.AnalyzePendingJobsUseCase;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PendingJobsAnalysisJob {

    private final AnalyzePendingJobsUseCase analyzePendingJobsUseCase;

    public PendingJobsAnalysisJob(AnalyzePendingJobsUseCase analyzePendingJobsUseCase) {
        this.analyzePendingJobsUseCase = analyzePendingJobsUseCase;
    }

    @Scheduled(every = "{vagora.analysis.every}", delayed = "20s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void analyze() {
        try {
            analyzePendingJobsUseCase.execute();
        } catch (Exception e) {
            Log.warn("Falha ao analisar as vagas pendentes", e);
        }
    }
}
