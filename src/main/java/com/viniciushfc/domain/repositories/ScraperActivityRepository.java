package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.ScraperActivity;

public interface ScraperActivityRepository {

    ScraperActivity get(AlertSource source);

    void save(AlertSource source, ScraperActivity activity);
}
