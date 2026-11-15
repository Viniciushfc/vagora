package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

public interface AnalyzeJobsMatchCvGateway {

    CvMatchResult analyze(JobDetail jobDetail, String cvUser, String apiKey);
}
