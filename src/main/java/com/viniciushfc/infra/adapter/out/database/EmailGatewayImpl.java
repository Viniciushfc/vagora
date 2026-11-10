package com.viniciushfc.infra.adapter.out.database;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.gateway.EmailGateway;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EmailGatewayImpl implements EmailGateway {

    @Override
    public void save(EmailReceived emailReceived) {

    }

    @Override
    public void delete(UUID uuidEmail) {

    }

    @Override
    public List<EmailReceived> findAll() {
        return List.of();
    }

    @Override
    public EmailReceived findById(UUID id) {
        return null;
    }

    @Override
    public boolean existsEmailByMessageId(String messageID) {
        return false;
    }
}