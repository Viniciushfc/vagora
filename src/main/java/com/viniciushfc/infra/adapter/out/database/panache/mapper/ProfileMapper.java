package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.vo.LocalLlm;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheProfileEntity;

import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toDomainAttachment;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toPanacheAttachmentEmbeddable;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toDomainLinkedAccounts;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toPanacheLinkedAccountEmbeddable;

public class ProfileMapper {

    public static Profile toDomain(PanacheProfileEntity panacheProfileEntity) {
        if (panacheProfileEntity == null) {
            return null;
        }

        return new Profile(
                panacheProfileEntity.getId(),
                panacheProfileEntity.getName(),
                panacheProfileEntity.getGeminiApiKey(),
                toDomainAttachment(panacheProfileEntity.getCurriculum()),
                toDomainLinkedAccounts(panacheProfileEntity.getLinkedAccounts()),
                panacheProfileEntity.getShowBrowser(),
                panacheProfileEntity.getMonitoringEnabled(),
                panacheProfileEntity.getGroqApiKey(),
                panacheProfileEntity.getLlmProvider(),
                panacheProfileEntity.getAutoCurriculum(),
                panacheProfileEntity.getPollMinMinutes(),
                panacheProfileEntity.getPollMaxMinutes(),
                panacheProfileEntity.getAnalysisChain(),
                panacheProfileEntity.getCurriculumChain(),
                panacheProfileEntity.getLocalLlm(),
                panacheProfileEntity.getClaudeApiKey(),
                panacheProfileEntity.getModels()
        );
    }

    public static PanacheProfileEntity toPanacheProfile(Profile profile) {
        return new PanacheProfileEntity(
                profile.id(),
                profile.name(),
                profile.geminiApiKey(),
                toPanacheAttachmentEmbeddable(profile.curriculum()),
                toPanacheLinkedAccountEmbeddable(profile.linkedAccounts()),
                profile.showBrowser(),
                profile.monitoringEnabled(),
                profile.groqApiKey(),
                profile.llmProvider(),
                profile.autoCurriculum(),
                profile.pollMinMinutes(),
                profile.pollMaxMinutes(),
                profile.analysisChain(),
                profile.curriculumChain(),
                profile.localLlm(),
                profile.claudeApiKey(),
                profile.models()
        );
    }

    public static PanacheProfileEntity mergeForUpdate(PanacheProfileEntity entityPersist, Profile bo) {
        if (entityPersist == null || bo == null) {
            return entityPersist;
        }

        if (bo.name() != null) {
            entityPersist.setName(bo.name());
        }

        if (bo.monitoringEnabled() != null) {
            entityPersist.setMonitoringEnabled(bo.monitoringEnabled());
        }

        if (bo.showBrowser() != null) {
            entityPersist.setShowBrowser(bo.showBrowser());
        }

        if (bo.geminiApiKey() != null) {
            entityPersist.setGeminiApiKey(bo.geminiApiKey());
        }

        if (bo.groqApiKey() != null) {
            entityPersist.setGroqApiKey(bo.groqApiKey());
        }

        if (bo.claudeApiKey() != null) {
            entityPersist.setClaudeApiKey(blankToNull(bo.claudeApiKey()));
        }

        if (bo.models() != null) {
            entityPersist.setModels(bo.models());
        }

        if (bo.llmProvider() != null) {
            entityPersist.setLlmProvider(bo.llmProvider());
        }

        if (bo.autoCurriculum() != null) {
            entityPersist.setAutoCurriculum(bo.autoCurriculum());
        }

        if (bo.pollMinMinutes() != null) {
            entityPersist.setPollMinMinutes(bo.pollMinMinutes());
        }

        if (bo.pollMaxMinutes() != null) {
            entityPersist.setPollMaxMinutes(bo.pollMaxMinutes());
        }

        if (bo.analysisChain() != null) {
            entityPersist.setAnalysisChain(bo.analysisChain());
        }

        if (bo.curriculumChain() != null) {
            entityPersist.setCurriculumChain(bo.curriculumChain());
        }

        if (bo.curriculum() != null) {
            entityPersist.setCurriculum(toPanacheAttachmentEmbeddable(bo.curriculum()));
        }

        LocalLlm localLlm = bo.localLlm();
        if (localLlm != null) {
            if (localLlm.url() != null) {
                entityPersist.setLocalLlmUrl(blankToNull(localLlm.url()));
            }
            if (localLlm.model() != null) {
                entityPersist.setLocalLlmModel(blankToNull(localLlm.model()));
            }
            if (localLlm.apiKey() != null) {
                entityPersist.setLocalLlmApiKey(blankToNull(localLlm.apiKey()));
            }
        }

        return entityPersist;
    }

    private static String blankToNull(String text) {
        return text.isBlank() ? null : text.strip();
    }
}
