package com.viniciushfc.infra.adapter.out.llm.gateway;

import com.viniciushfc.domain.gateway.AnalyzeJobsMatchCvGateway;
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
    public List<CvMatchResult> executeAnalysis(List<JobDetail> jobDetails, String cvUser) {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Detalhes das vagas \n")
                .append(jobDetails.toString())
                .append("\n")
                .append(cvUser);

        return geminiServiceImpl.genareteContentText(stringBuilder.toString());
    }
}
