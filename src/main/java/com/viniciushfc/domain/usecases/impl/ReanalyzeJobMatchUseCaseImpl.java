package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ReanalyzeJobMatchUseCase;
import com.viniciushfc.domain.vo.CvMatchResult;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.UUID;

@UseCase
public class ReanalyzeJobMatchUseCaseImpl implements ReanalyzeJobMatchUseCase {

    private static final System.Logger LOG = System.getLogger(ReanalyzeJobMatchUseCaseImpl.class.getName());

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;

    public ReanalyzeJobMatchUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository,
                                        AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public JobMatch execute(UUID jobMatchId) {
        Profile profile = profileRepository.require();
        JobMatch jobMatch = jobMatchRepository.findById(jobMatchId);
        if (jobMatch == null || !jobMatch.profileId().equals(profile.id())) {
            throw new NotFoundException("Vaga não encontrada.");
        }
        if (jobMatch.jobDescription() == null) {
            throw new BusinessException("Esta vaga foi analisada antes de o Vagora guardar a descrição, então não dá para refazer a análise.");
        }

        String curriculum = pdfGateway.extractTextPdf(profile.curriculum());
        CvMatchResult result = analyzeJobsMatchCvGateway.analyze(List.of(jobMatch.jobDetail()), curriculum, profile).getFirst();
        jobMatchRepository.updateAnalysis(jobMatch.id(), result);
        JobMatch analyzed = jobMatch.withAnalysis(result);

        try {
            String adapted = analyzeJobsMatchCvGateway.adaptCurriculum(analyzed.jobDetail(), curriculum, profile);
            jobMatchRepository.updateCurriculum(analyzed.id(), adapted);
            return analyzed.withCurriculum(adapted);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Nota refeita, mas o currículo adaptado novo não foi gerado; fica o anterior", e);
            return analyzed;
        }
    }
}
