package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntitiesTest {

    private Email email(String from) {
        return new Email(from, "to@x.com", "assunto", "corpo", List.of());
    }

    @Test
    void isAlertJobSomenteParaRemetenteDeAlertaDoLinkedin() {
        assertTrue(EmailReceived.create("m", email("LinkedIn <jobalerts-noreply@linkedin.com>")).isAlertJob());
        assertFalse(EmailReceived.create("m", email("outro@linkedin.com")).isAlertJob());
        assertFalse(EmailReceived.create("m", email(null)).isAlertJob());
    }

    @Test
    void findLinkedAccountPorProvider() {
        var email = LinkedAccount.create("e@x.com", "s", ExternalProvider.EMAIL, "INBOX");
        var user = Profile.create("F", "k", null);
        user.linkedAccounts().add(email);

        assertEquals(email, user.findLinkedAccount(ExternalProvider.EMAIL));
        assertNull(user.findLinkedAccount(ExternalProvider.LINKEDIN));
    }

}
