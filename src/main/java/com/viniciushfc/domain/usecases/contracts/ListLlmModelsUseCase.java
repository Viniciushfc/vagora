package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.LlmModels;

import java.util.List;

public interface ListLlmModelsUseCase {

    List<String> execute(LlmProvider provider);

    LlmModels defaults();
}
