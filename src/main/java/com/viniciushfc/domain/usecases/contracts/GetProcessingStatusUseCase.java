package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.ProcessingStatus;

import java.util.Map;

public interface GetProcessingStatusUseCase {
    Map<AlertSource, ProcessingStatus> execute();
}
