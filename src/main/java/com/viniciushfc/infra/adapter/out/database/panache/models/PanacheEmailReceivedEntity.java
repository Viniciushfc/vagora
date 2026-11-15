package com.viniciushfc.infra.adapter.out.database.panache.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Só o necessário para não processar o mesmo alerta duas vezes: o conteúdo do email não é guardado. */
@Entity
@Table(name = "email_received")
public class PanacheEmailReceivedEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String messageId;

    private boolean successProcessed;

    public PanacheEmailReceivedEntity(UUID id, String messageId, boolean successProcessed) {
        this.id = id;
        this.messageId = messageId;
        this.successProcessed = successProcessed;
    }

    public PanacheEmailReceivedEntity() {

    }

    public UUID getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isSuccessProcessed() {
        return successProcessed;
    }

    public void setSuccessProcessed(boolean successProcessed) {
        this.successProcessed = successProcessed;
    }
}
