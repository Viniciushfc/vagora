package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.vo.Email;
import com.viniciushfc.infra.adapter.out.database.panache.models.PanacheEmalReceivedEntity;

public class EmailReceivedMapper {

    public static EmailReceived toDomain(PanacheEmalReceivedEntity entity) {
        Email email = new Email(
                entity.getFrom(),
                entity.getTo(),
                entity.getSubject(),
                entity.getBody(),
                entity.getAttachments().stream().map(AttachmentMapper::toDomainAttachment).toList()
        );

        return new EmailReceived(entity.getId(), entity.getMessageId(), email, entity.isSuccessProcessed());
    }

    public static PanacheEmalReceivedEntity toPanacheEmailReceived(EmailReceived emailReceived) {
        Email email = emailReceived.email();

        return new PanacheEmalReceivedEntity(
                emailReceived.id(),
                emailReceived.messageId(),
                email.from(),
                email.to(),
                email.subject(),
                email.body(),
                email.attachments().stream().map(AttachmentMapper::toPanacheAttachmentEmbeddable).toList(),
                emailReceived.successProcessed()
        );
    }

}
