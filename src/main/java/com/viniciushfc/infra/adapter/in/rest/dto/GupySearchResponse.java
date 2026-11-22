package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record GupySearchResponse(UUID id,
                                 String jobName,
                                 String state,
                                 String city,
                                 Set<WorkplaceType> workplaces,
                                 Set<ContractType> contracts,
                                 int everyHours,
                                 boolean enabled,
                                 LocalDateTime lastRunAt) {

    public static GupySearchResponse from(GupySearch search) {
        return new GupySearchResponse(search.id(), search.jobName(), search.state(), search.city(), search.workplaces(),
                search.contracts(), search.everyHours(), search.enabled(), search.lastRunAt());
    }
}
