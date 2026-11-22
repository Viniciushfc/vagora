package com.viniciushfc.infra.adapter.out.memory;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.vo.ScraperActivity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class InMemoryScraperActivityRepository implements ScraperActivityRepository {

    private final Map<AlertSource, ScraperActivity> activities = new ConcurrentHashMap<>();

    @Override
    public ScraperActivity get(AlertSource source) {
        return activities.getOrDefault(source, ScraperActivity.idle());
    }

    @Override
    public void save(AlertSource source, ScraperActivity activity) {
        activities.put(source, activity);
    }

    @Override
    public void update(AlertSource source, UnaryOperator<ScraperActivity> change) {
        activities.compute(source, (key, current) -> change.apply(current == null ? ScraperActivity.idle() : current));
    }
}
