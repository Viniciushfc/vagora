package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;

public interface AnalyzeJobsMatchCvGateway {

    List<CvMatchResult> analyze(List<JobDetail> jobs, String cvUser, Profile profile);

    String adaptCurriculum(JobDetail jobDetail, String cvUser, Profile profile);
}
