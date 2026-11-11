package com.viniciushfc.infra.adapter.out.llm.gemini.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentResponse;
import com.viniciushfc.domain.vo.CvMatchResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GeminiCvMatchMapper {

    private final ObjectMapper objectMapper;

    public GeminiCvMatchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CvMatchResult toDomain(GenerateContentResponse response) {
        String text = response.text();

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Gemini response is empty.");
        }

        String json = removeMarkdownFences(text);

        try {
            return objectMapper.readValue(json, CvMatchResult.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Gemini response is not valid " + json, e);
        }
    }

    private String removeMarkdownFences(String text) {
        return text
                .replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```\\s*$", "")
                .trim();
    }
}