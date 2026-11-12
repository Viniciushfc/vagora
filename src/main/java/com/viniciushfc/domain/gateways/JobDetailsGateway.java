package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;
import java.util.Set;

public interface JobDetailsGateway {

    List<JobDetail> extractJobsDetails(Set<String> jobLinks);

}
