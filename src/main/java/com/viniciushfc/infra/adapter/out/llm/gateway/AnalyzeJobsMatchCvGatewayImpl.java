package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class AnalyzeJobsMatchCvGatewayImpl implements AnalyzeJobsMatchCvGateway {

    private final GeminiServiceImpl geminiServiceImpl;

    public AnalyzeJobsMatchCvGatewayImpl(GeminiServiceImpl geminiServiceImpl) {
        this.geminiServiceImpl = geminiServiceImpl;
    }

    @Override
    public List<CvMatchResult> analyze(List<JobDetail> jobs, String cvUser, String apiKey) {
        // o currículo vem primeiro: o começo igual entre chamadas aproveita o cache automático do Gemini
        StringBuilder prompt = new StringBuilder(curriculum(cvUser));
        for (int i = 0; i < jobs.size(); i++) {
            prompt.append(job(jobs.get(i), i + 1)).append('\n');
        }
        return geminiServiceImpl.analyze(prompt.toString(), jobs.size(), apiKey);
    }

    @Override
    public String adaptCurriculum(JobDetail jobDetail, String cvUser, String apiKey) {
        return geminiServiceImpl.adaptCurriculum(curriculum(cvUser) + job(jobDetail, 1), apiKey);
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
                company == null ? "" : Objects.toString(company.description(), ""));
    }

    static String curriculum(String cvUser) {
        return "<curriculo>\n" + cvUser + "\n</curriculo>\n";
    }
}
