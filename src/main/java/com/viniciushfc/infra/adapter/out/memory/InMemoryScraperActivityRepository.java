package com.viniciushfc.infra.adapter.out.memory;

import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.vo.ScraperActivity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class InMemoryScraperActivityRepository implements ScraperActivityRepository {

    private final AtomicReference<ScraperActivity> activity = new AtomicReference<>(ScraperActivity.idle());

    @Override
    public ScraperActivity get() {
        return activity.get();
    }

    @Override
    public void save(ScraperActivity activity) {
        this.activity.set(activity);
    }
}
