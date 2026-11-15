package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;

public interface AnalyzeJobsMatchCvGateway {

    /** Um resultado por vaga, na mesma ordem de {@code jobDetails}. */
    List<CvMatchResult> executeAnalysis(List<JobDetail> jobDetails, String cvUser, String apiKey);
}
