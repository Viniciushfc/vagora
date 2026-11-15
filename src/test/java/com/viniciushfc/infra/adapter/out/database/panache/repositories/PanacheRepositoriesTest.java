package com.viniciushfc.infra.adapter.out.database.panache.repositories;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.UserAccountRepository;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.Email;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheEmailReceivedRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PanacheRepositoriesTest {

    @Inject
    UserAccountRepository userAccountRepository;

    @Inject
    EmailReceivedRepository emailReceivedRepository;

    @Inject
    PanacheEmailReceivedRepository panacheEmailReceivedRepository;

    @Inject
    JobMatchRepository jobMatchRepository;

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1, 2, 3});

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@gmail.com";
    }

    @Test
    void userAccountSalvaBuscaAtualizaERemove() {
        var linkedUsername = unique("linked");
        var user = UserAccount.create("Fulano", unique("user"), "123", cv);
        user.linkedAccounts().add(LinkedAccount.create(linkedUsername, "x", ExternalProvider.LINKEDIN));

        userAccountRepository.save(user);

        var found = userAccountRepository.findById(user.id());
        assertEquals("Fulano", found.name());
        assertArrayEquals(cv.content(), found.curriculum().content());
        assertEquals(1, found.linkedAccounts().size());

        assertEquals(user.id(), userAccountRepository.findByLinkedUsername(linkedUsername).id());
        assertNull(userAccountRepository.findByLinkedUsername(unique("ninguem")));

        var updated = userAccountRepository.update(user.id(), new UserAccount(null, "Novo", null, null, null, null));
        assertEquals("Novo", updated.name());
        assertEquals(user.username(), updated.username());
        assertEquals("Novo", userAccountRepository.findById(user.id()).name());

        userAccountRepository.delete(user.id());
        assertNull(userAccountRepository.findById(user.id()));
    }

    @Test
    void userAccountUpdateDeInexistenteRetornaNulo() {
        assertNull(userAccountRepository.update(UUID.randomUUID(), new UserAccount(null, "x", null, null, null, null)));
    }

    @Test
    void emailReceivedSalvaEMarcaComoProcessado() {
        var email = EmailReceived.create(UUID.randomUUID().toString(),
                new Email("from@x.com", "to@x.com", "assunto", "corpo", List.of(cv)));

        emailReceivedRepository.save(email);
        assertFalse(isSuccessProcessed(email.id()));
        assertTrue(emailReceivedRepository.existsEmailByMessageId(email.messageId()));
        assertFalse(emailReceivedRepository.existsEmailByMessageId(UUID.randomUUID().toString()));
        assertNull(emailReceivedRepository.findByMessageId(UUID.randomUUID().toString()));

        var found = emailReceivedRepository.findByMessageId(email.messageId());
        assertEquals(email.id(), found.id());
        assertEquals("corpo", found.email().body());
        assertEquals(1, found.email().attachments().size());

        emailReceivedRepository.markSuccessProcessed(email.id());
        assertTrue(isSuccessProcessed(email.id()));
    }

    private boolean isSuccessProcessed(UUID id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> panacheEmailReceivedRepository.findById(id).isSuccessProcessed());
    }

    @Test
    void emailReceivedMarcarInexistenteNaoFalha() {
        assertDoesNotThrow(() -> emailReceivedRepository.markSuccessProcessed(UUID.randomUUID()));
    }

    @Test
    void jobMatchSalvaBuscaPorUsuarioEVerificaExistencia() {
        var userId = UUID.randomUUID();
        var outroUserId = UUID.randomUUID();
        var match1 = new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/",
                "Dev", "ACME", true, 80, List.of("a", "b"), "cv ats", LocalDateTime.now());
        var match2 = new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/2/",
                "QA", "ACME", false, 30, List.of("c"), null, LocalDateTime.now());
        var deOutro = new JobMatch(UUID.randomUUID(), outroUserId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/",
                "Dev", "ACME", true, 80, List.of("a"), "cv", LocalDateTime.now());

        jobMatchRepository.saveAll(List.of(match1, match2, deOutro));

        var matches = jobMatchRepository.findByUserAccountId(userId);
        assertEquals(2, matches.size());
        var found = matches.stream().filter(m -> m.id().equals(match1.id())).findFirst().orElseThrow();
        assertEquals(List.of("a", "b"), found.motivos());
        assertEquals("cv ats", found.curriculoAts());

        assertTrue(jobMatchRepository.existsByUserAccountIdAndJobLink(userId, "https://www.linkedin.com/jobs/view/1/"));
        assertFalse(jobMatchRepository.existsByUserAccountIdAndJobLink(userId, "https://www.linkedin.com/jobs/view/3/"));
        assertFalse(jobMatchRepository.existsByUserAccountIdAndJobLink(UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/"));
    }
}
