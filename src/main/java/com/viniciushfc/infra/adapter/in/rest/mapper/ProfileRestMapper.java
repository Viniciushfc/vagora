package com.viniciushfc.infra.adapter.in.rest.mapper;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.LocalLlm;
import com.viniciushfc.domain.vo.LlmModels;
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
                toChain(form.curriculumChain),
                form.localLlmUrl == null && form.localLlmModel == null && form.localLlmApiKey == null
                        ? null : new LocalLlm(form.localLlmUrl, form.localLlmModel, form.localLlmApiKey),
                form.claudeApiKey,
                form.geminiModel == null && form.groqModel == null && form.claudeApiModel == null && form.claudeCodeModel == null
                        ? null : new LlmModels(form.geminiModel, form.groqModel, form.claudeApiModel, form.claudeCodeModel),
                toChain(form.chatChain)
        );
    }

    public static LinkedAccount toDomain(LinkedAccountRequest request) {
        return new LinkedAccount(null, request.username(), request.password(), request.externalProvider(), request.folder(), null, null);
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.id(),
                profile.name(),
                profile.llmProviders().contains(LlmProvider.GEMINI),
                profile.llmProviders().contains(LlmProvider.GROQ),
                profile.claudeApiKey() != null && !profile.claudeApiKey().isBlank(),
                profile.primaryLlm(),
                profile.isBrowserVisible(),
                profile.isMonitoringEnabled(),
                profile.isAutoCurriculum(),
                profile.pollMin(),
                profile.pollMax(),
                profile.analysisOrder(),
                profile.curriculumOrder(),
                profile.chatOrder(),
                profile.localLlm() == null ? null : profile.localLlm().url(),
                profile.localLlm() == null ? null : profile.localLlm().model(),
                profile.localLlm() != null && profile.localLlm().apiKey() != null,
                profile.llmProviders().contains(LlmProvider.LOCAL),
                profile.models() == null ? new LlmModels(null, null, null, null) : profile.models(),
                profile.curriculum() == null ? null : profile.curriculum().name(),
                profile.curriculum() == null || profile.curriculum().content() == null
                        ? null : profile.curriculum().content().length,
                profile.linkedAccounts().stream()
                        .map(l -> new LinkedAccountResponse(l.id(), l.username(), l.externalProvider(), l.folder(), l.indeedFolder(), l.glassdoorFolder()))
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
