package com.viniciushfc.infra.adapter.out.memory;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.vo.ProcessingStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryProcessingStatusRepository implements ProcessingStatusRepository {

    private record Key(UUID profileId, AlertSource source) {
    }

    private final Map<Key, ProcessingStatus> statuses = new ConcurrentHashMap<>();

    @Override
    public ProcessingStatus find(UUID profileId, AlertSource source) {
        return statuses.getOrDefault(new Key(profileId, source), ProcessingStatus.empty());
    }

    @Override
    public void save(UUID profileId, AlertSource source, ProcessingStatus status) {
        statuses.put(new Key(profileId, source), status);
    }
}
