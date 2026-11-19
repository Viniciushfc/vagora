package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.GenerateCurriculumUseCase;
import com.viniciushfc.domain.vo.Attachment;

import java.util.UUID;

@UseCase
public class GenerateCurriculumUseCaseImpl implements GenerateCurriculumUseCase {

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;

    public GenerateCurriculumUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository,
                                         AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public JobMatch execute(UUID jobMatchId) {
        Profile profile = profileRepository.require();
        JobMatch jobMatch = find(profile, jobMatchId);
        if (jobMatch.jobDescription() == null) {
            throw new BusinessException("Esta vaga foi analisada antes de o Vagora guardar a descrição, então não dá para gerar o currículo.");
        }
        if (profile.llmProviders().isEmpty()) {
            throw new BusinessException(
                    "Configure uma IA em Configurações: a chave do Gemini ou do Groq, ou conecte o Claude.", ErrorCode.LLM_KEY_MISSING);
        }

        String adapted = analyzeJobsMatchCvGateway.adaptCurriculum(jobMatch.jobDetail(),
                pdfGateway.extractTextPdf(profile.curriculum()), profile);
        jobMatchRepository.updateCurriculum(jobMatch.id(), adapted);
        return jobMatch.withCurriculum(adapted);
    }

    @Override
    public Attachment pdf(UUID jobMatchId) {
        JobMatch jobMatch = find(profileRepository.require(), jobMatchId);
        if (jobMatch.curriculoAts() == null || jobMatch.curriculoAts().isBlank()) {
            throw new NotFoundException("Esta vaga ainda não tem currículo adaptado.");
        }
        return new Attachment("Currículo - " + jobMatch.jobTitle() + ".pdf", "application/pdf",
                pdfGateway.createPdf(jobMatch.curriculoAts()));
    }

    private JobMatch find(Profile profile, UUID jobMatchId) {
        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId);
        if (jobMatch == null || !jobMatch.profileId().equals(profile.id())) {
            throw new NotFoundException("Vaga não encontrada.");
        }
        return jobMatch;
    }
}
