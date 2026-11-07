package com.viniciushfc.infra.adapter.out.scraper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.gateway.JobDetailsGateway;
import com.viniciushfc.domain.vo.JobDetail;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class JobDetailsGatewayImpl implements JobDetailsGateway {

    private final ObjectMapper mapper;
    private final String linkedinLogin;
    private final String linkedinPassword;

    public JobDetailsGatewayImpl(ObjectMapper mapper,
                                 @ConfigProperty(name = "LINKEDIN_LOGIN") String linkedinLogin,
                                 @ConfigProperty(name = "LINKEDIN_PASSWORD") String linkedinPassword) {
        this.mapper = mapper;
        this.linkedinLogin = linkedinLogin;
        this.linkedinPassword = linkedinPassword;
    }

    @Override
    public List<JobDetail> extractJobsDetails(Set<String> jobLinks) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python/.venv/bin/python", "python/scraping_detalis_job.py")
                    .redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.environment().put("LINKEDIN_LOGIN", linkedinLogin);
            pb.environment().put("LINKEDIN_PASSWORD", linkedinPassword);

            Process process = pb.start();
            mapper.writeValue(process.getOutputStream(), jobLinks);
            return mapper.readValue(process.getInputStream(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
