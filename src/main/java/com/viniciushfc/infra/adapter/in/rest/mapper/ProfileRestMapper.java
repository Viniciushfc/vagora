package com.viniciushfc.infra.adapter.in.rest.mapper;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.in.rest.dto.LinkedAccountRequest;
import com.viniciushfc.infra.adapter.in.rest.dto.LinkedAccountResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.ProfileForm;
import com.viniciushfc.infra.adapter.in.rest.dto.ProfileResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class ProfileRestMapper {

    public static Profile toDomain(ProfileForm form) {
        return new Profile(
                null,
                form.name,
                form.geminiApiKey,
                toAttachment(form.curriculum),
                null,
                form.showBrowser,
                form.monitoringEnabled,
                form.groqApiKey,
                form.llmProvider,
                form.autoCurriculum,
                form.pollMinMinutes,
                form.pollMaxMinutes,
                toChain(form.analysisChain),
                toChain(form.curriculumChain)
        );
    }

    public static LinkedAccount toDomain(LinkedAccountRequest request) {
        return new LinkedAccount(null, request.username(), request.password(), request.externalProvider(), request.folder(), null);
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.id(),
                profile.name(),
                profile.llmProviders().contains(LlmProvider.GEMINI),
                profile.llmProviders().contains(LlmProvider.GROQ),
                profile.primaryLlm(),
                profile.isBrowserVisible(),
                profile.isMonitoringEnabled(),
                profile.isAutoCurriculum(),
                profile.pollMin(),
                profile.pollMax(),
                profile.analysisOrder(),
                profile.curriculumOrder(),
                profile.curriculum() == null ? null : profile.curriculum().name(),
                profile.curriculum() == null || profile.curriculum().content() == null
                        ? null : profile.curriculum().content().length,
                profile.linkedAccounts().stream()
                        .map(l -> new LinkedAccountResponse(l.id(), l.username(), l.externalProvider(), l.folder(), l.indeedFolder()))
                        .toList()
        );
    }

    private static List<LlmProvider> toChain(String chain) {
        if (chain == null) {
            return null;
        }
        try {
            return Arrays.stream(chain.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(LlmProvider::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("IA desconhecida no fluxo: " + chain + ".");
        }
    }

    private static Attachment toAttachment(FileUpload file) {
        if (file == null) {
            return null;
        }

        try {
            return new Attachment(file.fileName(), file.contentType(), Files.readAllBytes(file.uploadedFile()));
        } catch (IOException e) {
            throw new UncheckedIOException("Erro lendo currículo", e);
        }
    }
}
