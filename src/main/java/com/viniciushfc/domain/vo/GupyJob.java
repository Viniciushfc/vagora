package com.viniciushfc.domain.vo;

import com.viniciushfc.domain.enums.ContractType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GupyJob(JobDetail job, ContractType contract, LocalDateTime publishedAt, LocalDate deadline) {
}
