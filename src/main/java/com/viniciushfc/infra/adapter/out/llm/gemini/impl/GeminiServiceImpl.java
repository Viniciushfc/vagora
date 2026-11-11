package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.gemini.contracts.GeminiService;
import com.viniciushfc.infra.adapter.out.llm.gemini.mapper.GeminiCvMatchMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@ApplicationScoped
public class GeminiServiceImpl implements GeminiService {

    private final Client client;

    private final GenerateContentConfig config;

    private final GeminiCvMatchMapper cvMatchMapper;

    public GeminiServiceImpl(@ConfigProperty(name = "llm.gemini.api") String apiKey, GeminiCvMatchMapper cvMatchMapper) {
        this.client = Client
                .builder()
                .apiKey(apiKey)
                .build();
        this.config = GenerateContentConfig.builder()
                .temperature(0.2f)
                .responseMimeType("application/json")
                .systemInstruction(Content.fromParts(Part.fromText(PromptsContextsLLM.AnalyzerCvMatcherPrompt)))
                .build();
        ;
        this.cvMatchMapper = cvMatchMapper;
    }

    @Override
    public CvMatchResult genareteContentText(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                "gemini-3.8-flash",
                prompt,
                config);

        return cvMatchMapper.toDomain(response);
    }
}
