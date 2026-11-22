package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.GupySearch;

import java.util.List;

public interface ListGupySearchesUseCase {
    List<GupySearch> execute();
}
