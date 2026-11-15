package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.vo.ScraperActivity;

public interface ScraperActivityRepository {

    ScraperActivity get();

    void save(ScraperActivity activity);
}
