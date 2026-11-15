package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheEmailReceivedEntity;

public class EmailReceivedMapper {

    /** O email lido do banco não tem conteúdo: só id, Message-ID e status. */
    public static EmailReceived toDomain(PanacheEmailReceivedEntity entity) {
        return new EmailReceived(entity.getId(), entity.getMessageId(), null, entity.isSuccessProcessed());
    }

    public static PanacheEmailReceivedEntity toPanacheEmailReceived(EmailReceived emailReceived) {
        return new PanacheEmailReceivedEntity(emailReceived.id(), emailReceived.messageId(), emailReceived.successProcessed());
    }
}
