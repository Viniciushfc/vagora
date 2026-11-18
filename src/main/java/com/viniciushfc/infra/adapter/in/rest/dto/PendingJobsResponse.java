package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.vo.ProcessingStatus;

import java.util.List;

public record PendingJobsResponse(List<PendingJobResponse> jobs,
                                  ProcessingStatus analysis,
                                  int maxAttempts) {
}
