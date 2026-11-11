package com.viniciushfc.domain.gateway;

import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;

public interface AnalyzeJobsMatchCvGateway {

    List<CvMatchResult> executeAnalysis(List<JobDetail> jobDetails, String cvUser);
}
