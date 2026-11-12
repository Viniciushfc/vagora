package com.viniciushfc.infra.adapter.out.database.panache.repositories.impl;

import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PanacheUserAccountRepository implements UserAccountRepository {

    @Override
    public UserAccount save(UserAccount userAccount) {
        return null;
    }

    @Override
    public void delete(UUID idUser) {

    }

    @Override
    public UserAccount update(UUID id, UserAccount userAccount) {
        return null;
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
