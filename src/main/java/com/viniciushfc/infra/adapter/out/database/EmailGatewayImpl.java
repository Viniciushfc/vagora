package com.viniciushfc.infra.adapter.out.database;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.gateway.EmailGateway;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EmailGatewayImpl implements EmailGateway {
    @Override
    public void save(EmailReceived emailReceived) {

    }

    @Override
    public void delete(EmailReceived emailReceived) {

    }

    @Override
    public List<EmailReceived> findAll() {
        return List.of();
    }

    @Override
    public EmailReceived findById(String id) {
        return null;
    }

    @Override
    public boolean existsEmailByMessageId(String messageID) {
        return false;
    }
}