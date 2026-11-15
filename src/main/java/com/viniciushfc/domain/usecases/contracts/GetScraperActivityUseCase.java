package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.vo.ScraperActivity;

public interface GetScraperActivityUseCase {
    ScraperActivity execute();

    byte[] failureScreenshot();
}
