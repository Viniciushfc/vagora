package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record GupySearch(UUID id,
                         UUID profileId,
                         String jobName,
                         String state,
                         String city,
                         Set<WorkplaceType> workplaces,
                         Set<ContractType> contracts,
                         int everyHours,
                         boolean enabled,
                         LocalDateTime lastRunAt) {

    public static final int DEFAULT_EVERY_HOURS = 4;

    public boolean isDue(LocalDateTime now) {
        return enabled && (lastRunAt == null || !now.isBefore(lastRunAt.plusHours(everyHours)));
    }

    public GupySearch ranAt(LocalDateTime now) {
        return new GupySearch(id, profileId, jobName, state, city, workplaces, contracts, everyHours, enabled, now);
    }
}
