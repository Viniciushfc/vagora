package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.vo.Email;

import java.util.UUID;

public record EmailReceived(UUID id,
                            String messageId,
                            Email email) {

    public static EmailReceived create(String messageId, Email email) {
        return new EmailReceived(UUID.randomUUID(), messageId, email);
    }

    public boolean isAlertJob(){
        return email.from() != null && email.from().contains("jobalerts-noreply@linkedin.com");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EmailReceived other && id.equals(other.id);
    }

}
