package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;

import java.util.List;

public interface ProcessEmailReceiverUseCase {
    boolean execute(Profile profile, AlertSource source, List<EmailReceived> alerts);
}
