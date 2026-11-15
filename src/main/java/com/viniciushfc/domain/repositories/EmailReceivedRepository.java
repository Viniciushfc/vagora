package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.EmailReceived;

import java.util.UUID;

public interface EmailReceivedRepository {
    void save(EmailReceived emailReceived);

    EmailReceived findByMessageId(String messageId);

    void markSuccessProcessed(UUID id);
}
