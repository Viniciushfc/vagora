package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AnalyzeJobsMatchCvGatewayImpl implements AnalyzeJobsMatchCvGateway {

    private final GeminiServiceImpl geminiServiceImpl;

    public AnalyzeJobsMatchCvGatewayImpl(GeminiServiceImpl geminiServiceImpl) {
        this.geminiServiceImpl = geminiServiceImpl;
    }

    @Override
    public List<CvMatchResult> analyze(List<JobDetail> jobs, String cvUser, String apiKey) {
        StringBuilder prompt = new StringBuilder();
        for (int i = 0; i < jobs.size(); i++) {
            prompt.append(job(jobs.get(i), i + 1)).append('\n');
        }
        prompt.append(curriculum(cvUser));
        return geminiServiceImpl.analyze(prompt.toString(), jobs.size(), apiKey);
    }

    @Override
    public String adaptCurriculum(JobDetail jobDetail, String cvUser, String apiKey) {
        return geminiServiceImpl.adaptCurriculum(job(jobDetail, 1) + "\n" + curriculum(cvUser), apiKey);
    }

    static String job(JobDetail job, int id) {
        var company = job.companyDetails();
        return """
                <vaga id="%d">
                Título: %s
                Empresa: %s
                Descrição: %s
                Sobre a empresa: %s
                </vaga>""".formatted(id, job.title(), company == null ? "" : company.name(), job.description(),
                company == null ? "" : company.description());
    }

    static String curriculum(String cvUser) {
        return "<curriculo>\n" + cvUser + "\n</curriculo>\n";
    }
}
