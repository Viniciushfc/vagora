package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.EmailReceived;

import java.io.IOException;

public interface ProcessEmailReceiverUseCase {
    void execute(EmailReceived emailReceived) throws IOException;
}
