package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.Email;

import java.util.UUID;

public record EmailReceived(UUID id,
                            String messageId,
                            Email email,
                            boolean successProcessed) {

    public static EmailReceived create(String messageId, Email email) {
        return new EmailReceived(UUID.randomUUID(), messageId, email, false);
    }

    public boolean isFrom(AlertSource source) {
        return email.from() != null && email.from().contains(source.sender);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EmailReceived other && id.equals(other.id);
    }

}
