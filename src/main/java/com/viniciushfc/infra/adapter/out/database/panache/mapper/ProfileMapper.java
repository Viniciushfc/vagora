package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheProfileEntity;

import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toDomainAttachment;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toPanacheAttachmentEmbeddable;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toDomainLinkedAccounts;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toPanacheLinkedAccountEmbeddable;

public class ProfileMapper {

    public static Profile toDomain(PanacheProfileEntity panacheProfileEntity) {
        if (Utils.isNull(panacheProfileEntity)) {
            return null;
        }

        return new Profile(
                panacheProfileEntity.getId(),
                panacheProfileEntity.getName(),
                panacheProfileEntity.getGeminiApiKey(),
                toDomainAttachment(panacheProfileEntity.getCurriculum()),
                toDomainLinkedAccounts(panacheProfileEntity.getLinkedAccounts()),
                panacheProfileEntity.getShowBrowser(),
                panacheProfileEntity.getMonitoringEnabled()
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
                profile.monitoringEnabled()
        );
    }


    public static PanacheProfileEntity mergeForUpdate(PanacheProfileEntity entityPersist, Profile bo) {

        if (Utils.isNull(entityPersist) || Utils.isNull(bo)) {
            return entityPersist;
        }

        if (!Utils.isNull(bo.name())) {
            entityPersist.setName(bo.name());
        }

        if (!Utils.isNull(bo.monitoringEnabled())){
            entityPersist.setMonitoringEnabled(bo.monitoringEnabled());
        }

        if (!Utils.isNull(bo.showBrowser())){
            entityPersist.setShowBrowser(bo.showBrowser());
        }

        if (!Utils.isNull(bo.geminiApiKey())){
            entityPersist.setGeminiApiKey(bo.geminiApiKey());
        }

        if (!Utils.isNull(bo.curriculum())){
            entityPersist.setCurriculum(toPanacheAttachmentEmbeddable(bo.curriculum()));
        }

        return entityPersist;
    }

}
