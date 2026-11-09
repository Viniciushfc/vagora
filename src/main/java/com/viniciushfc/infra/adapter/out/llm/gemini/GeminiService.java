package com.viniciushfc.infra.adapter.out.llm.gemini;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;


@ApplicationScoped
public class GeminiService {

    private final Client client;

    private final GenerateContentConfig config;

    private GeminiCvMatchMapper cvMatchMapper;

    public GeminiService(@ConfigProperty(name = "llm.gemini.api") String apiKey, GenerateContentConfig config, GeminiCvMatchMapper cvMatchMapper) {
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

    public CvMatchResult genareteContentText(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                "gemini-3.8-flash",
                prompt,
                config);

        return cvMatchMapper.toDomain(response);
    }
}
