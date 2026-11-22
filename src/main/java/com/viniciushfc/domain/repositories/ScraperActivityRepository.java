package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.ScraperActivity;

import java.util.function.UnaryOperator;

public interface ScraperActivityRepository {

    ScraperActivity get(AlertSource source);

    void save(AlertSource source, ScraperActivity activity);

    void update(AlertSource source, UnaryOperator<ScraperActivity> change);
}
