package com.viniciushfc.infra.adapter.out.database.panache.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_match")
public class PanacheJobMatchEntity {

    @Id
    private UUID id;
    private UUID userAccountId;
    private UUID emailReceivedId;
    private String jobLink;
    private String jobTitle;
    private String companyName;
    @Column(name = "is_match")
    private boolean match;
    private int score;

    @ElementCollection
    @CollectionTable(
            name = "job_match_motivo",
            joinColumns = @JoinColumn(name = "id_job_match", referencedColumnName = "id")
    )
    @Column(name = "motivo", columnDefinition = "TEXT")
    private List<String> motivos;

    @Column(columnDefinition = "TEXT")
    private String curriculoAts;

    private LocalDateTime createdAt;

    public PanacheJobMatchEntity(UUID id, UUID userAccountId, UUID emailReceivedId, String jobLink, String jobTitle, String companyName, boolean match, int score, List<String> motivos, String curriculoAts, LocalDateTime createdAt) {
        this.id = id;
        this.userAccountId = userAccountId;
        this.emailReceivedId = emailReceivedId;
        this.jobLink = jobLink;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.match = match;
        this.score = score;
        this.motivos = motivos;
        this.curriculoAts = curriculoAts;
        this.createdAt = createdAt;
    }

    public PanacheJobMatchEntity() {

    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public UUID getEmailReceivedId() {
        return emailReceivedId;
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

    public boolean isMatch() {
        return match;
    }

    public int getScore() {
        return score;
    }

    public List<String> getMotivos() {
        return motivos;
    }

    public String getCurriculoAts() {
        return curriculoAts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
