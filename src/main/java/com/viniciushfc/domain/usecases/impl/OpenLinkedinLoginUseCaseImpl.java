package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.OpenLinkedinLoginUseCase;

@UseCase
public class OpenLinkedinLoginUseCaseImpl implements OpenLinkedinLoginUseCase {

    private final ProfileRepository profileRepository;
    private final JobDetailsGateway jobDetailsGateway;
    private final ProcessingStatusRepository statusRepository;

    public OpenLinkedinLoginUseCaseImpl(ProfileRepository profileRepository, JobDetailsGateway jobDetailsGateway,
                                        ProcessingStatusRepository statusRepository) {
        this.profileRepository = profileRepository;
        this.jobDetailsGateway = jobDetailsGateway;
        this.statusRepository = statusRepository;
    }

    @Override
    public void execute() {
        Profile profile = profileRepository.require();
        LinkedAccount linkedin = profile.findLinkedAccount(ExternalProvider.LINKEDIN);
        if (linkedin == null) {
            throw new BusinessException("Conecte uma conta do LinkedIn em Contas.", ErrorCode.LINKEDIN_NOT_CONNECTED);
        }
        jobDetailsGateway.openLoginWindow(linkedin,
                () -> statusRepository.save(profile.id(), AlertSource.LINKEDIN,
                        statusRepository.find(profile.id(), AlertSource.LINKEDIN).waitUntil(null)));
    }
}
