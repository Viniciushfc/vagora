package com.viniciushfc.infra.adapter.out.database.panache.models;

import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheAttachmentEmbeddable;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "email_received")
public class PanacheEmalReceivedEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String messageId;

    @Column(name = "email_from")
    private String from;

    @Column(name = "email_to")
    private String to;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ElementCollection
    @CollectionTable(
            name = "email_received_attachment",
            joinColumns = @JoinColumn(name = "id_email_received", referencedColumnName = "id")
    )
    private List<PanacheAttachmentEmbeddable> attachments;

    private boolean successProcessed;

    public PanacheEmalReceivedEntity(UUID id, String messageId, String from, String to, String subject, String body, List<PanacheAttachmentEmbeddable> attachments, boolean successProcessed) {
        this.id = id;
        this.messageId = messageId;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;
        this.successProcessed = successProcessed;
    }

    public PanacheEmalReceivedEntity() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<PanacheAttachmentEmbeddable> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<PanacheAttachmentEmbeddable> attachments) {
        this.attachments = attachments;
    }

    public boolean isSuccessProcessed() {
        return successProcessed;
    }

    public void setSuccessProcessed(boolean successProcessed) {
        this.successProcessed = successProcessed;
    }
}
