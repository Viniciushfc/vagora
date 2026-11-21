package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheProfileEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PanacheMappersTest {

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1, 2});

    private Profile user() {
        return new Profile(UUID.randomUUID(), "Fulano", "chave", cv,
                List.of(LinkedAccount.create("f@gmail.com", "x", ExternalProvider.EMAIL, "LinkedIn")), null, null, null, null, null, null, null, null, null);
    }

    @Test
    void profileIdaEVolta() {
        var user = user();

        var back = ProfileMapper.toDomain(ProfileMapper.toPanacheProfile(user));

        assertEquals(user.id(), back.id());
        assertEquals(user.name(), back.name());
        assertEquals("chave", back.geminiApiKey());
        assertEquals(cv.name(), back.curriculum().name());
        assertArrayEquals(cv.content(), back.curriculum().content());
        assertEquals(user.linkedAccounts(), back.linkedAccounts());
    }

    @Test
    void profileToDomainNuloRetornaNulo() {
        assertNull(ProfileMapper.toDomain(null));
    }

    @Test
    void profileSemCurriculo() {
        var user = new Profile(UUID.randomUUID(), "F", null, null, List.of(), null, null, null, null, null, null, null, null, null);

        assertNull(ProfileMapper.toDomain(ProfileMapper.toPanacheProfile(user)).curriculum());
    }

    @Test
    void mergeForUpdateAlteraSomenteCamposInformados() {
        PanacheProfileEntity entity = ProfileMapper.toPanacheProfile(user());

        ProfileMapper.mergeForUpdate(entity, new Profile(null, "Novo", null, null, null, null, null, null, null, null, null, null, null, null));

        assertEquals("Novo", entity.getName());
        assertEquals("chave", entity.getGeminiApiKey());
        assertEquals("cv.pdf", entity.getCurriculum().name());
        assertEquals(1, entity.getLinkedAccounts().size());
    }

    @Test
    void mergeForUpdateSubstituiCurriculoEChave() {
        PanacheProfileEntity entity = ProfileMapper.toPanacheProfile(user());
        var novoCv = new Attachment("novo.pdf", "application/pdf", new byte[]{9});

        ProfileMapper.mergeForUpdate(entity, new Profile(null, null, "nova-chave", novoCv, null, null, null, null, null, null, null, null, null, null));

        assertEquals("nova-chave", entity.getGeminiApiKey());
        assertEquals("novo.pdf", entity.getCurriculum().name());
    }

    @Test
    void jobMatchIdaEVolta() {
        var match = new JobMatch(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "link", "Dev", "ACME",
                true, 77, List.of("a", "b"), "cv ats", LocalDateTime.now(), null);

        assertEquals(match.toString(), JobMatchMapper.toDomain(JobMatchMapper.toPanacheJobMatch(match)).toString());
    }
}
