package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntitiesTest {

    private Email email(String from) {
        return new Email(from, "to@x.com", "assunto", "corpo", List.of());
    }

    @Test
    void isFromSomenteParaORemetenteDeAlertaDaFonte() {
        assertTrue(EmailReceived.create("m", email("LinkedIn <jobalerts-noreply@linkedin.com>")).isFrom(AlertSource.LINKEDIN));
        assertTrue(EmailReceived.create("m", email("Indeed <donotreply@match.indeed.com>")).isFrom(AlertSource.INDEED));
        assertFalse(EmailReceived.create("m", email("Indeed <donotreply@match.indeed.com>")).isFrom(AlertSource.LINKEDIN));
        assertFalse(EmailReceived.create("m", email("outro@linkedin.com")).isFrom(AlertSource.LINKEDIN));
        assertFalse(EmailReceived.create("m", email(null)).isFrom(AlertSource.INDEED));
    }

    @Test
    void findLinkedAccountPorProvider() {
        var email = LinkedAccount.create("e@x.com", "s", ExternalProvider.EMAIL, "INBOX");
        var user = Profile.create("F", "k", null, null, null);
        user.linkedAccounts().add(email);

        assertEquals(email, user.findLinkedAccount(ExternalProvider.EMAIL));
        assertNull(user.findLinkedAccount(ExternalProvider.LINKEDIN));
    }

}
