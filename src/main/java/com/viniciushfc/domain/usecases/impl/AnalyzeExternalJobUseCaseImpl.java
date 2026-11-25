package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.AnalyzeExternalJobUseCase;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.lang.System.Logger.Level;
import java.util.List;

@UseCase
public class AnalyzeExternalJobUseCaseImpl implements AnalyzeExternalJobUseCase {

    private static final System.Logger LOG = System.getLogger(AnalyzeExternalJobUseCaseImpl.class.getName());

    private final ProfileRepository profileRepository;
    private final JobMatchRepository jobMatchRepository;
    private final AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway;
    private final PdfGateway pdfGateway;

    public AnalyzeExternalJobUseCaseImpl(ProfileRepository profileRepository, JobMatchRepository jobMatchRepository,
                                         AnalyzeJobsMatchCvGateway analyzeJobsMatchCvGateway, PdfGateway pdfGateway) {
        this.profileRepository = profileRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.analyzeJobsMatchCvGateway = analyzeJobsMatchCvGateway;
        this.pdfGateway = pdfGateway;
    }

    @Override
    public JobMatch execute(JobDetail pasted) {
        if (blank(pasted.title()) || blank(pasted.description())) {
            throw new ValidationException("Informe o título e a descrição da vaga.");
        }
        String company = pasted.companyDetails() == null ? null : pasted.companyDetails().name();
        JobDetail job = new JobDetail(blank(pasted.link()) ? null : pasted.link().strip(), pasted.title().strip(),
                pasted.description().strip(), new CompanyDetails(blank(company) ? null : company.strip(), null));

        Profile profile = profileRepository.require();
        String curriculum = pdfGateway.extractTextPdf(profile.curriculum());
        CvMatchResult result = analyzeJobsMatchCvGateway.analyze(List.of(job), curriculum, profile).getFirst();
        JobMatch jobMatch = JobMatch.create(profile.id(), null, job, result);
        jobMatchRepository.saveAll(List.of(jobMatch));

        try {
            String adapted = analyzeJobsMatchCvGateway.adaptCurriculum(job, curriculum, profile);
            jobMatchRepository.updateCurriculum(jobMatch.id(), adapted);
            return jobMatch.withCurriculum(adapted);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Currículo da vaga colada não gerado; fica o botão Gerar currículo", e);
            return jobMatch;
        }
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
