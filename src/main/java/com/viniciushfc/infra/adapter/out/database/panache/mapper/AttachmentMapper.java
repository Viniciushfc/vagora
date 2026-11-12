package com.viniciushfc.infra.adapter.out.database.panache.mapper;

import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheAttachmentEmbeddable;

public class AttachmentMapper {

    public static Attachment toDomainAttachment(PanacheAttachmentEmbeddable panacheAttachmentEmbeddable) {
        return new Attachment(
                panacheAttachmentEmbeddable.name(),
                panacheAttachmentEmbeddable.contentType(),
                panacheAttachmentEmbeddable.content()
        );
    }

    public static PanacheAttachmentEmbeddable toPanacheAttachmentEmbeddable(Attachment attachment) {
        return new PanacheAttachmentEmbeddable(
                attachment.name(),
                attachment.contentType(),
                attachment.content()
        );
    }

}
