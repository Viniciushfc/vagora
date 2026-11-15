package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.vo.ProcessingStatus;

import java.util.UUID;

public interface ProcessingStatusRepository {

    ProcessingStatus find(UUID profileId);

    void save(UUID profileId, ProcessingStatus status);
}
