package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.gemini.mapper.GeminiCvMatchMapper;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GeminiServiceImpl {

    private final GenerateContentConfig config;

    private final GeminiCvMatchMapper cvMatchMapper;

    public GeminiServiceImpl(GeminiCvMatchMapper cvMatchMapper) {
        this.config = GenerateContentConfig.builder()
                .temperature(0.2f)
                .responseMimeType("application/json")
                .systemInstruction(Content.fromParts(Part.fromText(PromptsContextsLLM.AnalyzerCvMatcherPrompt)))
                .build();
        ;
        this.cvMatchMapper = cvMatchMapper;
    }

    public CvMatchResult analyze(String prompt, String apiKey) {
        // cada perfil usa a própria chave
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.8-flash",
                    prompt,
                    config);

            return cvMatchMapper.toDomain(response);
        }
    }
}
