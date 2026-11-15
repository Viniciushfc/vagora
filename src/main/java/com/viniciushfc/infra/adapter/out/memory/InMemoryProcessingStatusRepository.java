package com.viniciushfc.infra.adapter.out.memory;

import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.vo.ProcessingStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Em memória de propósito: o estado se renova a cada ciclo de verificação,
 * então não precisa sobreviver a um reinício.
 */
@ApplicationScoped
public class InMemoryProcessingStatusRepository implements ProcessingStatusRepository {

    private final Map<UUID, ProcessingStatus> statuses = new ConcurrentHashMap<>();

    @Override
    public ProcessingStatus find(UUID profileId) {
        return statuses.getOrDefault(profileId, ProcessingStatus.empty());
    }

    @Override
    public void save(UUID profileId, ProcessingStatus status) {
        statuses.put(profileId, status);
    }
}
