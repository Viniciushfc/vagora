package com.viniciushfc.infra.adapter.out.database.panache.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "email_received")
public class PanacheEmailReceivedEntity {

    @Id
    private UUID id;

    @Column(unique = true)
    private String messageId;

    private boolean successProcessed;

    @Column(columnDefinition = "TEXT")
    private String scrapedJobs;

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

    public String getScrapedJobs() {
        return scrapedJobs;
    }

    public void setScrapedJobs(String scrapedJobs) {
        this.scrapedJobs = scrapedJobs;
    }
}
