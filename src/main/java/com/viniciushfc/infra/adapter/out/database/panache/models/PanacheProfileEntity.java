package com.viniciushfc.infra.adapter.out.database.panache.models;

import com.viniciushfc.domain.enums.LlmProvider;
import com.viniciushfc.domain.vo.LocalLlm;
import com.viniciushfc.domain.vo.LlmModels;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheAttachmentEmbeddable;
import com.viniciushfc.infra.adapter.out.database.panache.embeddable.PanacheLinkedAccountEmbeddable;
import jakarta.persistence.*;

import java.util.Arrays;
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
    private Integer pollMinMinutes;
    private Integer pollMaxMinutes;
    private String analysisLlmChain;
    private String curriculumLlmChain;
    private Boolean showBrowser;
    private Boolean monitoringEnabled;
    private String localLlmUrl;
    private String localLlmModel;
    private String localLlmApiKey;
    private String claudeApiKey;
    private String geminiModel;
    private String groqModel;
    private String claudeApiModel;
    private String claudeCodeModel;

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
                                Boolean autoCurriculum,
                                Integer pollMinMinutes,
                                Integer pollMaxMinutes,
                                List<LlmProvider> analysisChain,
                                List<LlmProvider> curriculumChain,
                                LocalLlm localLlm,
                                String claudeApiKey,
                                LlmModels models) {
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
        this.pollMinMinutes = pollMinMinutes;
        this.pollMaxMinutes = pollMaxMinutes;
        setAnalysisChain(analysisChain);
        setCurriculumChain(curriculumChain);
        this.claudeApiKey = claudeApiKey;
        if (models != null) {
            setModels(models);
        }
        if (localLlm != null) {
            this.localLlmUrl = localLlm.url();
            this.localLlmModel = localLlm.model();
            this.localLlmApiKey = localLlm.apiKey();
        }
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

    public Integer getPollMinMinutes() {
        return pollMinMinutes;
    }

    public void setPollMinMinutes(Integer pollMinMinutes) {
        this.pollMinMinutes = pollMinMinutes;
    }

    public Integer getPollMaxMinutes() {
        return pollMaxMinutes;
    }

    public void setPollMaxMinutes(Integer pollMaxMinutes) {
        this.pollMaxMinutes = pollMaxMinutes;
    }

    public List<LlmProvider> getAnalysisChain() {
        return fromColumn(analysisLlmChain);
    }

    public void setAnalysisChain(List<LlmProvider> chain) {
        this.analysisLlmChain = toColumn(chain);
    }

    public List<LlmProvider> getCurriculumChain() {
        return fromColumn(curriculumLlmChain);
    }

    public void setCurriculumChain(List<LlmProvider> chain) {
        this.curriculumLlmChain = toColumn(chain);
    }

    private static List<LlmProvider> fromColumn(String column) {
        return column == null ? null : Arrays.stream(column.split(",")).map(LlmProvider::valueOf).toList();
    }

    private static String toColumn(List<LlmProvider> chain) {
        return chain == null ? null : String.join(",", chain.stream().map(Enum::name).toList());
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

    public LocalLlm getLocalLlm() {
        return localLlmUrl == null && localLlmModel == null && localLlmApiKey == null
                ? null : new LocalLlm(localLlmUrl, localLlmModel, localLlmApiKey);
    }

    public void setLocalLlmUrl(String localLlmUrl) {
        this.localLlmUrl = localLlmUrl;
    }

    public void setLocalLlmModel(String localLlmModel) {
        this.localLlmModel = localLlmModel;
    }

    public void setLocalLlmApiKey(String localLlmApiKey) {
        this.localLlmApiKey = localLlmApiKey;
    }

    public String getClaudeApiKey() {
        return claudeApiKey;
    }

    public void setClaudeApiKey(String claudeApiKey) {
        this.claudeApiKey = claudeApiKey;
    }

    public LlmModels getModels() {
        return new LlmModels(geminiModel, groqModel, claudeApiModel, claudeCodeModel);
    }

    public void setModels(LlmModels models) {
        if (models.gemini() != null) {
            geminiModel = blankToNull(models.gemini());
        }
        if (models.groq() != null) {
            groqModel = blankToNull(models.groq());
        }
        if (models.claudeApi() != null) {
            claudeApiModel = blankToNull(models.claudeApi());
        }
        if (models.claudeCode() != null) {
            claudeCodeModel = blankToNull(models.claudeCode());
        }
    }

    private static String blankToNull(String text) {
        return text.isBlank() ? null : text.strip();
    }
}
