package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.UserAccount;

import java.util.UUID;

public interface UserAccountRepository {

    UserAccount save(UserAccount userAccount);

    void delete(UUID idUser);

    UserAccount update(UUID id, UserAccount userAccount);

    UserAccount findById(UUID id);

    UserAccount findByLinkedUsername(String username);
}
