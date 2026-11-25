package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ChatJobMatchUseCase;
import com.viniciushfc.domain.vo.ChatMessage;

import java.util.List;
import java.util.UUID;

@UseCase
public class ChatJobMatchUseCaseImpl implements ChatJobMatchUseCase {

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;

    public ChatJobMatchUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository,
                                   AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public String execute(UUID jobMatchId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty() || !messages.getLast().fromUser()
                || messages.getLast().text() == null || messages.getLast().text().isBlank()) {
            throw new ValidationException("Escreva uma mensagem para a IA.");
        }
        Profile profile = profileRepository.require();
        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId);
        if (jobMatch == null || !jobMatch.profileId().equals(profile.id())) {
            throw new NotFoundException("Vaga não encontrada.");
        }
        if (jobMatch.jobDescription() == null) {
            throw new BusinessException("Esta vaga foi analisada antes de o Vagora guardar a descrição, então não dá para conversar sobre ela.");
        }
        return analyzeJobsMatchCvGateway.chat(jobMatch, pdfGateway.extractTextPdf(profile.curriculum()), messages, profile);
    }
}
