package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.ProcessingStatus;

import java.util.UUID;

public interface ProcessingStatusRepository {

    ProcessingStatus find(UUID profileId, AlertSource source);

    void save(UUID profileId, AlertSource source, ProcessingStatus status);

    ProcessingStatus findAnalysis(UUID profileId);

    void saveAnalysis(UUID profileId, ProcessingStatus status);
}
