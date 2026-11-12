package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.vo.Attachment;

import java.util.List;
import java.util.UUID;

public record UserAccount(UUID id,
                          String name,
                          String username,
                          String password,
                          Attachment curriculum,
                          List<LinkedAccount> linkedAccounts) {

    public UserAccount(UUID id, String name, String username, String password, Attachment curriculum, List<LinkedAccount> linkedAccounts) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.curriculum = curriculum;
        this.linkedAccounts = linkedAccounts;
    }
}
