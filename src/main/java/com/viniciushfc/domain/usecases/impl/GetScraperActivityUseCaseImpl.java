package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.usecases.contracts.GetScraperActivityUseCase;
import com.viniciushfc.domain.vo.ScraperActivity;

@UseCase
public class GetScraperActivityUseCaseImpl implements GetScraperActivityUseCase {

    private final ScraperActivityRepository scraperActivityRepository;
    private final JobDetailsGateway jobDetailsGateway;

    public GetScraperActivityUseCaseImpl(ScraperActivityRepository scraperActivityRepository, JobDetailsGateway jobDetailsGateway) {
        this.scraperActivityRepository = scraperActivityRepository;
        this.jobDetailsGateway = jobDetailsGateway;
    }

    @Override
    public ScraperActivity execute() {
        return scraperActivityRepository.get();
    }

    @Override
    public byte[] failureScreenshot() {
        byte[] screenshot = jobDetailsGateway.lastFailureScreenshot();
        if (screenshot == null) {
            throw new NotFoundException("Nenhuma captura de tela disponível.");
        }
        return screenshot;
    }
}
