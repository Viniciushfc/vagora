package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.Email;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheUserAccountEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PanacheMappersTest {

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1, 2});

    private UserAccount user() {
        return new UserAccount(UUID.randomUUID(), "Fulano", "fulano", "123", cv,
                List.of(LinkedAccount.create("f@gmail.com", "x", ExternalProvider.LINKEDIN)));
    }

    @Test
    void userAccountIdaEVolta() {
        var user = user();

        var back = UserAccountMapper.toDomain(UserAccountMapper.toPanacheUserAccount(user));

        assertEquals(user.id(), back.id());
        assertEquals(user.name(), back.name());
        assertEquals(user.username(), back.username());
        assertEquals(user.password(), back.password());
        assertEquals(cv.name(), back.curriculum().name());
        assertArrayEquals(cv.content(), back.curriculum().content());
        assertEquals(user.linkedAccounts(), back.linkedAccounts());
    }

    @Test
    void userAccountToDomainNuloRetornaNulo() {
        assertNull(UserAccountMapper.toDomain(null));
    }

    @Test
    void userAccountSemCurriculo() {
        var user = new UserAccount(UUID.randomUUID(), "F", "f", "1", null, List.of());

        assertNull(UserAccountMapper.toDomain(UserAccountMapper.toPanacheUserAccount(user)).curriculum());
    }

    @Test
    void mergeForUpdateAlteraSomenteCamposInformados() {
        PanacheUserAccountEntity entity = UserAccountMapper.toPanacheUserAccount(user());

        UserAccountMapper.mergeForUpdate(entity, new UserAccount(null, "Novo", null, null, null, null));

        assertEquals("Novo", entity.getName());
        assertEquals("fulano", entity.getUsername());
        assertEquals("123", entity.getPassword());
        assertEquals("cv.pdf", entity.getCurriculum().name());
        assertEquals(1, entity.getLinkedAccounts().size());
    }

    @Test
    void mergeForUpdateSubstituiCurriculoEContas() {
        PanacheUserAccountEntity entity = UserAccountMapper.toPanacheUserAccount(user());
        var novoCv = new Attachment("novo.pdf", "application/pdf", new byte[]{9});
        var contas = List.of(
                LinkedAccount.create("a@gmail.com", "x", ExternalProvider.LINKEDIN),
                LinkedAccount.create("b@gmail.com", "y", ExternalProvider.LINKEDIN));

        UserAccountMapper.mergeForUpdate(entity, new UserAccount(null, null, null, "nova", novoCv, contas));

        assertEquals("nova", entity.getPassword());
        assertEquals("novo.pdf", entity.getCurriculum().name());
        assertEquals(2, entity.getLinkedAccounts().size());
    }

    @Test
    void emailReceivedIdaEVolta() {
        var email = new Email("from@x.com", "to@x.com", "assunto", "<b>corpo</b>", List.of(cv));
        var received = new EmailReceived(UUID.randomUUID(), "msg-1", email, true);

        var back = EmailReceivedMapper.toDomain(EmailReceivedMapper.toPanacheEmailReceived(received));

        assertEquals(received.id(), back.id());
        assertEquals("msg-1", back.messageId());
        assertTrue(back.successProcessed());
        assertEquals("from@x.com", back.email().from());
        assertEquals("to@x.com", back.email().to());
        assertEquals("assunto", back.email().subject());
        assertEquals("<b>corpo</b>", back.email().body());
        assertEquals(1, back.email().attachments().size());
        assertArrayEquals(cv.content(), back.email().attachments().getFirst().content());
    }

    @Test
    void jobMatchIdaEVolta() {
        var match = new JobMatch(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "link", "Dev", "ACME",
                true, 77, List.of("a", "b"), "cv ats", LocalDateTime.now());

        assertEquals(match.toString(), JobMatchMapper.toDomain(JobMatchMapper.toPanacheJobMatch(match)).toString());
    }
}
