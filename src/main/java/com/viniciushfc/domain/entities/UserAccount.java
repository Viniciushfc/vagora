package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.vo.Attachment;

import java.util.ArrayList;
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

    public static UserAccount create(String name, String username, String password, Attachment curriculum) {
        return new UserAccount(UUID.randomUUID(), name, username, password, curriculum, new ArrayList<>());
    }
}
