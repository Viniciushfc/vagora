package com.viniciushfc.infra.adapter.out.database.panache.models;

import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheAttachmentEmbeddable;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheLinkedAccountEmbeddable;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_account")
public class PanacheProfileEntity {

    @Id
    private UUID id;
    private String name;
    private String geminiApiKey;
    private String groqApiKey;
    private String llmProvider;
    private Boolean autoCurriculum;
    private Boolean showBrowser;
    private Boolean monitoringEnabled;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "nameAttachment")),
            @AttributeOverride(name = "contentType", column = @Column(name = "contentType")),
            @AttributeOverride(name = "content", column = @Column(name = "content", length = PanacheAttachmentEmbeddable.MAX_CONTENT_LENGTH))
    })
    private PanacheAttachmentEmbeddable curriculum;

    @ElementCollection
    @CollectionTable(
            name = "user_linked_account",
            joinColumns = @JoinColumn(name = "id_user_account", referencedColumnName = "id")
    )
    private List<PanacheLinkedAccountEmbeddable> linkedAccounts;

    public PanacheProfileEntity(UUID id,
                                String name,
                                String geminiApiKey,
                                PanacheAttachmentEmbeddable curriculum,
                                List<PanacheLinkedAccountEmbeddable> linkedAccounts,
                                Boolean showBrowser,
                                Boolean monitoringEnabled,
                                String groqApiKey,
                                LlmProvider llmProvider,
                                Boolean autoCurriculum) {
        this.id = id;
        this.name = name;
        this.geminiApiKey = geminiApiKey;
        this.curriculum = curriculum;
        this.linkedAccounts = linkedAccounts;
        this.showBrowser = showBrowser;
        this.monitoringEnabled = monitoringEnabled;
        this.groqApiKey = groqApiKey;
        setLlmProvider(llmProvider);
        this.autoCurriculum = autoCurriculum;
    }

    public PanacheProfileEntity() {

    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(Boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    public Boolean getShowBrowser() {
        return showBrowser;
    }

    public void setShowBrowser(Boolean showBrowser) {
        this.showBrowser = showBrowser;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public Boolean getAutoCurriculum() {
        return autoCurriculum;
    }

    public void setAutoCurriculum(Boolean autoCurriculum) {
        this.autoCurriculum = autoCurriculum;
    }

    public String getGroqApiKey() {
        return groqApiKey;
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    public LlmProvider getLlmProvider() {
        return llmProvider == null ? null : LlmProvider.valueOf(llmProvider);
    }

    public void setLlmProvider(LlmProvider llmProvider) {
        this.llmProvider = llmProvider == null ? null : llmProvider.name();
    }

    public PanacheAttachmentEmbeddable getCurriculum() {
        return curriculum;
    }

    public void setCurriculum(PanacheAttachmentEmbeddable curriculum) {
        this.curriculum = curriculum;
    }

    public List<PanacheLinkedAccountEmbeddable> getLinkedAccounts() {
        return linkedAccounts;
    }

    public void setLinkedAccounts(List<PanacheLinkedAccountEmbeddable> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }
};
