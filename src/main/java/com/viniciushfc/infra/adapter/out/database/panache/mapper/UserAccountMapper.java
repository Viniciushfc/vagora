package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheUserAccountEntity;

import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toDomainAttachment;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.AttachmentMapper.toPanacheAttachmentEmbeddable;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toDomainLinkedAccounts;
import static com.viniciushfc.infra.adapter.out.database.panache.mapper.LinkedAccountMapper.toPanacheLinkedAccountEmbeddable;

public class UserAccountMapper {

    public static UserAccount toDomain(PanacheUserAccountEntity panacheUserAccountEntity) {
        return new UserAccount(
                panacheUserAccountEntity.getId(),
                panacheUserAccountEntity.getName(),
                panacheUserAccountEntity.getUsername(),
                panacheUserAccountEntity.getPassword(),
                toDomainAttachment(panacheUserAccountEntity.getCurriculum()),
                toDomainLinkedAccounts(panacheUserAccountEntity.getLinkedAccounts())
        );
    }

    public static PanacheUserAccountEntity toPanacheUserAccount(UserAccount userAccount) {
        return new PanacheUserAccountEntity(
                userAccount.id(),
                userAccount.name(),
                userAccount.username(),
                userAccount.password(),
                toPanacheAttachmentEmbeddable(userAccount.curriculum()),
                toPanacheLinkedAccountEmbeddable(userAccount.linkedAccounts())
        );
    }


    public static PanacheUserAccountEntity mergeForUpdate(PanacheUserAccountEntity entityPersist, UserAccount bo) {

        if (Utils.isNull(entityPersist) || Utils.isNull(bo)) {
            return entityPersist;
        }

        if (!Utils.isNull(bo.name())) {
            entityPersist.setName(bo.name());
        }

        if (!Utils.isNull(bo.username())) {
            entityPersist.setUsername(bo.username());
        }

        if (!Utils.isNull(bo.password())){
            entityPersist.setPassword(bo.password());
        }

        if (!Utils.isNull(bo.curriculum())){
            entityPersist.setCurriculum(null);
        }

        if (!Utils.isNull(bo.password())){
            entityPersist.setLinkedAccounts(null);
        }

        return entityPersist;
    }

}
