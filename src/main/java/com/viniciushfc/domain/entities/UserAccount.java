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
}
