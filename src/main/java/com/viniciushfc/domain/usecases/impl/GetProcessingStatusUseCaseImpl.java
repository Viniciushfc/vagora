package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.GetProcessingStatusUseCase;
import com.viniciushfc.domain.vo.ProcessingStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@UseCase
public class GetProcessingStatusUseCaseImpl implements GetProcessingStatusUseCase {

    private final ProfileRepository profileRepository;
    private final ProcessingStatusRepository processingStatusRepository;

    public GetProcessingStatusUseCaseImpl(ProfileRepository profileRepository, ProcessingStatusRepository processingStatusRepository) {
        this.profileRepository = profileRepository;
        this.processingStatusRepository = processingStatusRepository;
    }

    @Override
    public Map<AlertSource, ProcessingStatus> execute() {
        UUID profileId = profileRepository.require().id();
        Map<AlertSource, ProcessingStatus> statuses = new EnumMap<>(AlertSource.class);
        for (AlertSource source : AlertSource.values()) {
            statuses.put(source, processingStatusRepository.find(profileId, source));
        }
        return statuses;
    }
}
