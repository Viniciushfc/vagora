package com.viniciushfc.domain.repositories;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.NotFoundException;

import java.util.List;
import java.util.UUID;

/** Cada instalação tem um único perfil. */
public interface ProfileRepository {

    Profile save(Profile profile);

    /** O perfil desta instalação, ou {@code null} se ainda não foi criado. */
    Profile find();

    Profile update(UUID id, Profile profile);

    Profile updateLinkedAccounts(UUID id, List<LinkedAccount> linkedAccounts);

    void delete(UUID id);

    default Profile require() {
        Profile profile = find();
        if (profile == null) {
            throw new NotFoundException("Nenhum perfil configurado.");
        }
        return profile;
    }
}
