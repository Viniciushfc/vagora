package com.viniciushfc.infra.adapter.in.rest.dto;

import com.viniciushfc.domain.enums.LlmProvider;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class ProfileForm {

    @RestForm
    public String name;

    @RestForm
    public String geminiApiKey;

    @RestForm
    public String groqApiKey;

    @RestForm
    public LlmProvider llmProvider;

    @RestForm
    public FileUpload curriculum;

    @RestForm
    public Boolean showBrowser;

    @RestForm
    public Boolean monitoringEnabled;

    @RestForm
    public Boolean autoCurriculum;

    @RestForm
    public Integer pollMinMinutes;

    @RestForm
    public Integer pollMaxMinutes;

    @RestForm
    public String analysisChain;

    @RestForm
    public String curriculumChain;

    @RestForm
    public String localLlmUrl;

    @RestForm
    public String localLlmModel;

    @RestForm
    public String localLlmApiKey;

    @RestForm
    public String claudeApiKey;
}
