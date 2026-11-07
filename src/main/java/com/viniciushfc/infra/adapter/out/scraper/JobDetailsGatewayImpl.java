package com.viniciushfc.infra.adapter.out.scraper;

import com.viniciushfc.domain.gateway.JobDetailsGateway;
import com.viniciushfc.domain.vo.JobDetail;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class JobDetailsGatewayImpl implements JobDetailsGateway {

    @Override
    public List<JobDetail> extractJobsDetails(Set<String> jobLinks) {
        return List.of();
    }
}
