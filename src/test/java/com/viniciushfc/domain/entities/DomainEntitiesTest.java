package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntitiesTest {

    private Email email(String from) {
        return new Email(from, "to@x.com", "assunto", "corpo", List.of());
    }

    @Test
    void emailReceivedCreateGeraIdENaoProcessado() {
        var received = EmailReceived.create("msg", email("a@x.com"));

        assertNotNull(received.id());
        assertEquals("msg", received.messageId());
        assertFalse(received.successProcessed());
    }

    @Test
    void isAlertJobSomenteParaRemetenteDeAlertaDoLinkedin() {
        assertTrue(EmailReceived.create("m", email("LinkedIn <jobalerts-noreply@linkedin.com>")).isAlertJob());
        assertFalse(EmailReceived.create("m", email("outro@linkedin.com")).isAlertJob());
        assertFalse(EmailReceived.create("m", email(null)).isAlertJob());
    }

    @Test
    void emailReceivedEqualsPorId() {
        var id = UUID.randomUUID();
        assertEquals(new EmailReceived(id, "a", email("x"), false), new EmailReceived(id, "b", email("y"), true));
        assertNotEquals(EmailReceived.create("a", email("x")), EmailReceived.create("a", email("x")));
    }

    @Test
    void jobMatchCreateCopiaDadosDaVagaEDoResultado() {
        var userId = UUID.randomUUID();
        var emailId = UUID.randomUUID();
        var job = new JobDetail("link", "Dev Java", "desc", new CompanyDetails("ACME", "sobre"));
        var result = new CvMatchResult(true, 80, List.of("bom"), "cv ats");

        var match = JobMatch.create(userId, emailId, job, result);

        assertNotNull(match.id());
        assertNotNull(match.createdAt());
        assertEquals(userId, match.userAccountId());
        assertEquals(emailId, match.emailReceivedId());
        assertEquals("link", match.jobLink());
        assertEquals("Dev Java", match.jobTitle());
        assertEquals("ACME", match.companyName());
        assertTrue(match.match());
        assertEquals(80, match.score());
        assertEquals(List.of("bom"), match.motivos());
        assertEquals("cv ats", match.curriculoAts());
    }

    @Test
    void userAccountCreateGeraIdEListaMutavelVazia() {
        var user = UserAccount.create("Fulano", "fulano", "123", null);

        assertNotNull(user.id());
        assertTrue(user.linkedAccounts().isEmpty());
        user.linkedAccounts().add(LinkedAccount.create("a", "b", ExternalProvider.LINKEDIN));
    }

    @Test
    void linkedAccountCreateGeraId() {
        var linked = LinkedAccount.create("a@x.com", "s", ExternalProvider.LINKEDIN);

        assertNotNull(linked.id());
        assertEquals("a@x.com", linked.username());
    }
}
