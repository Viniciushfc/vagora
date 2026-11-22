package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;

import java.util.Set;

public record GupySearchRequest(String jobName,
                                String state,
                                String city,
                                Set<WorkplaceType> workplaces,
                                Set<ContractType> contracts,
                                Integer everyHours,
                                Boolean enabled) {
}
