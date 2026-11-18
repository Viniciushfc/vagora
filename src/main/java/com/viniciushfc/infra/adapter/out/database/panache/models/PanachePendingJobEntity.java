package com.viniciushfc.infra.adapter.out.database.panache.models;

import com.viniciushfc.domain.enums.AlertSource;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_job")
public class PanachePendingJobEntity {

    @Id
    private UUID id;
    private UUID profileId;
    private UUID emailReceivedId;
    private String source;
    private String jobLink;
    private String jobTitle;
    private String companyName;
    @Column(columnDefinition = "TEXT")
    private String companyDescription;
    @Column(columnDefinition = "TEXT")
    private String jobDescription;
    private boolean prioritized;
    private int attempts;
    private LocalDateTime createdAt;

    public PanachePendingJobEntity(UUID id, UUID profileId, UUID emailReceivedId, AlertSource source, String jobLink,
                                   String jobTitle, String companyName, String companyDescription, String jobDescription,
                                   boolean prioritized, int attempts, LocalDateTime createdAt) {
        this.id = id;
        this.profileId = profileId;
        this.emailReceivedId = emailReceivedId;
        this.source = source.name();
        this.jobLink = jobLink;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.companyDescription = companyDescription;
        this.jobDescription = jobDescription;
        this.prioritized = prioritized;
        this.attempts = attempts;
        this.createdAt = createdAt;
    }

    public PanachePendingJobEntity() {

    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getEmailReceivedId() {
        return emailReceivedId;
    }

    public AlertSource getSource() {
        return AlertSource.valueOf(source);
    }

    public String getJobLink() {
        return jobLink;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public boolean isPrioritized() {
        return prioritized;
    }

    public void setPrioritized(boolean prioritized) {
        this.prioritized = prioritized;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
