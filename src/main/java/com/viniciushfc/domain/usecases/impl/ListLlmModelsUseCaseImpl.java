package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ListLlmModelsUseCase;
import com.viniciushfc.domain.vo.LlmModels;

import java.util.List;

@UseCase
public class ListLlmModelsUseCaseImpl implements ListLlmModelsUseCase {

    private final ProfileRepository profileRepository;
    private final AnalyzeJobsMatchCvGateway llmGateway;

    public ListLlmModelsUseCaseImpl(ProfileRepository profileRepository, AnalyzeJobsMatchCvGateway llmGateway) {
        this.profileRepository = profileRepository;
        this.llmGateway = llmGateway;
    }

    @Override
    public List<String> execute(LlmProvider provider) {
        return llmGateway.models(provider, profileRepository.require());
    }

    @Override
    public LlmModels defaults() {
        return llmGateway.defaultModels();
    }
}
