package com.viniciushfc.domain.gateway;

import com.viniciushfc.domain.entities.EmailReceived;

import java.util.List;
import java.util.UUID;

public interface EmailGateway {
    void save(EmailReceived emailReceived);

    void delete(UUID uuidEmail);

    List<EmailReceived> findAll();

    EmailReceived findById(UUID id);

    boolean existsEmailByMessageId(String messageID);
}
