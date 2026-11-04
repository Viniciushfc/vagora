package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.EmailReceived;

public interface ProcessEmailReceiverUseCase {
    void execute(EmailReceived emailReceived);
}
