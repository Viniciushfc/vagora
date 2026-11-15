package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.Profile;

public interface ProcessEmailReceiverUseCase {
    /** @return {@code true} se o LinkedIn foi acessado (o ritmo de segurança só conta esses casos) */
    boolean execute(Profile profile, EmailReceived emailReceived);
}
