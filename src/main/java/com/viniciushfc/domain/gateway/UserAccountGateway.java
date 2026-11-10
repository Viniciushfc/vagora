package com.viniciushfc.domain.gateway;

import com.viniciushfc.domain.entities.UserAccount;

import java.util.List;
import java.util.UUID;

public interface UserAccountGateway {

    void save(UserAccount userAccount);

    void delete(UUID idUser);

    List<UserAccount> findAll();

    UserAccount findById(UUID id);

    UserAccount findByLinkedUsername(String username);
}
