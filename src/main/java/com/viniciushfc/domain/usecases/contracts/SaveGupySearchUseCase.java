package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.GupySearch;

public interface SaveGupySearchUseCase {
    GupySearch execute(GupySearch search);
}
