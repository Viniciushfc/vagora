package com.viniciushfc.domain.gateway;

import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;
import java.util.Set;

public interface JobDetailsGateway {

    List<JobDetail> extractJobsDetails(Set<String> jobLinks);

}
