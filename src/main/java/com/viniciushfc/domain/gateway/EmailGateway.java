package com.viniciushfc.domain.gateway;

import com.viniciushfc.domain.entities.EmailReceived;

import java.util.List;

public interface EmailGateway {
    void save(EmailReceived emailReceived);

    void delete(EmailReceived emailReceived);

    List<EmailReceived> findAll();

    EmailReceived findById(String id);

    boolean existsEmailByMessageId(String messageID);
}
