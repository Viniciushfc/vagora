package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.enums.AlertSource;

public interface PollJobAlertsUseCase {
    void execute(AlertSource source);
}
