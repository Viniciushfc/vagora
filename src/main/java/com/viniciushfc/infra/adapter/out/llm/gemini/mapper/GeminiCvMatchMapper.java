package com.viniciushfc.infra.adapter.out.llm.gemini.mapper;

import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.types.GenerateContentResponse;
import com.viniciushfc.domain.vo.CvMatchResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Arrays;

@ApplicationScoped
public class GeminiCvMatchMapper {

    private final ObjectMapper objectMapper;

    public GeminiCvMatchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CvMatchResult> toDomain(GenerateContentResponse response, int expected) {
        String text = response.text();

        if (text == null || text.isBlank()) {
            throw new ExternalServiceException("O Gemini não respondeu à análise. O alerta será tentado de novo.");
        }

        String json = removeMarkdownFences(text);
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode items = node.has("resultados") ? node.get("resultados")
                    : node.isArray() ? node : objectMapper.createArrayNode().add(node);
            CvMatchResult[] results = new CvMatchResult[expected];
            for (JsonNode item : items) {
                int id = item.path("id").asInt(expected == 1 ? 1 : 0);
                if (id < 1 || id > expected || results[id - 1] != null) {
                    throw invalid(json, null);
                }
                results[id - 1] = objectMapper.treeToValue(((ObjectNode) item.deepCopy()).without("id"), CvMatchResult.class);
            }
            if (Arrays.asList(results).contains(null)) {
                throw invalid(json, null);
            }
            return List.of(results);
        } catch (JsonProcessingException e) {
            throw invalid(json, e);
        }
    }

    private ExternalServiceException invalid(String json, Exception cause) {
        Log.warnf(cause, "Resposta do Gemini fora do formato: %s", json);
        return new ExternalServiceException("O Gemini devolveu uma resposta fora do formato esperado. O alerta será tentado de novo.", cause);
    }

    private String removeMarkdownFences(String text) {
        return text
                .replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```\\s*$", "")
                .trim();
    }
}