package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.GetProcessingStatusUseCase;
import com.viniciushfc.domain.vo.ProcessingStatus;

@UseCase
public class GetProcessingStatusUseCaseImpl implements GetProcessingStatusUseCase {

    private final ProfileRepository profileRepository;
    private final ProcessingStatusRepository processingStatusRepository;

    public GetProcessingStatusUseCaseImpl(ProfileRepository profileRepository, ProcessingStatusRepository processingStatusRepository) {
        this.profileRepository = profileRepository;
        this.processingStatusRepository = processingStatusRepository;
    }

    @Override
    public ProcessingStatus execute() {
        return processingStatusRepository.find(profileRepository.require().id());
    }
}
