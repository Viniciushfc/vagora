package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.ScraperActivity;

import java.util.Map;

public interface GetScraperActivityUseCase {
    Map<AlertSource, ScraperActivity> execute();

    byte[] failureScreenshot();
}
