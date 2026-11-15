package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.Profile;

public interface ProcessEmailReceiverUseCase {
    void execute(Profile profile, EmailReceived emailReceived);
}
