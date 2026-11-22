package com.viniciushfc.infra.adapter.in.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record JobMatchResponse(UUID id,
                               String jobLink,
                               String jobTitle,
                               String companyName,
                               boolean match,
                               int score,
                               List<String> motivos,
                               String curriculoAts,
                               LocalDateTime createdAt,
                               boolean canGenerateCurriculum,
                               LocalDateTime appliedAt) {
}
