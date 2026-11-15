package com.viniciushfc.infra.adapter.out.llm.gemini.mapper;

import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
            throw new ExternalServiceException("Gemini retornou uma resposta vazia.");
        }

        String json = removeMarkdownFences(text);

        try {
            JsonNode node = objectMapper.readTree(json);
            // o prompt pede um objeto; se o modelo devolver uma lista com ele, aceita o primeiro
            if (node.isArray() && !node.isEmpty()) {
                node = node.get(0);
            }
            return objectMapper.treeToValue(node, CvMatchResult.class);
        } catch (JsonProcessingException e) {
            throw new ExternalServiceException("Gemini retornou um JSON inválido: " + json, e);
        }
    }

    private String removeMarkdownFences(String text) {
        return text
                .replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```\\s*$", "")
                .trim();
    }
}