package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.llm.gemini.impl.GeminiServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Uma chamada ao Gemini por vaga, no formato que o prompt espera (uma vaga, um JSON):
 * assim a nota nunca é atribuída à vaga errada.
 */
@ApplicationScoped
public class AnalyzeJobsMatchCvGatewayImpl implements AnalyzeJobsMatchCvGateway {

    private final GeminiServiceImpl geminiServiceImpl;

    public AnalyzeJobsMatchCvGatewayImpl(GeminiServiceImpl geminiServiceImpl) {
        this.geminiServiceImpl = geminiServiceImpl;
    }

    @Override
    public List<CvMatchResult> executeAnalysis(List<JobDetail> jobDetails, String cvUser, String apiKey) {
        return jobDetails.stream()
                .map(job -> geminiServiceImpl.analyze(prompt(job, cvUser), apiKey))
                .toList();
    }

    static String prompt(JobDetail job, String cvUser) {
        var company = job.companyDetails();
        return """
                <vaga>
                Título: %s
                Empresa: %s
                Descrição: %s
                Sobre a empresa: %s
                </vaga>
                <curriculo>
                %s
                </curriculo>
                """.formatted(job.title(), company == null ? "" : company.name(), job.description(),
                company == null ? "" : company.description(), cvUser);
    }
}
