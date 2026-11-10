package com.viniciushfc.infra.adapter.out.database;

import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.gateway.UserAccountGateway;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UserAccountGatewayImpl implements UserAccountGateway {
    @Override
    public void save(UserAccount userAccount) {

    }

    @Override
    public void delete(UUID idUser) {

    }

    @Override
    public List<UserAccount> findAll() {
        return List.of();
    }

    @Override
    public UserAccount findById(UUID id) {
        return null;
    }

    @Override
    public UserAccount findByLinkedUsername(String username) {
        return null;
    }
}
