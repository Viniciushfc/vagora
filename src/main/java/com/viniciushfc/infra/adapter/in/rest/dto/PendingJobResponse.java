package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.AlertSource;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingJobResponse(UUID id,
                                 AlertSource source,
                                 String jobLink,
                                 String jobTitle,
                                 String companyName,
                                 boolean prioritized,
                                 int attempts,
                                 LocalDateTime createdAt) {
}
