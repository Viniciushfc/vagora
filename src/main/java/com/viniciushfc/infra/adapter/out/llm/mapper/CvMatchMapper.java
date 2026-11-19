package com.viniciushfc.infra.adapter.out.llm.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.vo.CvMatchResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CvMatchMapper {

    private static final Map<String, Object> RESULT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "id", Map.of("type", "integer"),
                    "match", Map.of("type", "boolean"),
                    "score", Map.of("type", "integer"),
                    "motivos", Map.of("type", "array", "items", Map.of("type", "string"))),
            "required", List.of("id", "match", "score", "motivos"),
            "additionalProperties", false);

    public static final Map<String, Object> ANALYSIS_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("resultados", Map.of("type", "array", "items", RESULT_SCHEMA)),
            "required", List.of("resultados"),
            "additionalProperties", false);

    private final ObjectMapper objectMapper;

    public CvMatchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CvMatchResult> toDomain(String provider, String text, int expected) {
        if (text == null || text.isBlank()) {
            String message = "O %s não respondeu à análise. O alerta será tentado de novo.".formatted(provider);
            throw new ExternalServiceException(message, ErrorCode.LLM_INVALID_RESPONSE);
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
                    throw invalid(provider, json, null);
                }
                ObjectNode withoutId = ((ObjectNode) item.deepCopy()).without("id");
                results[id - 1] = objectMapper.treeToValue(withoutId, CvMatchResult.class);
            }
            if (Arrays.asList(results).contains(null)) {
                throw invalid(provider, json, null);
            }
            return List.of(results);
        } catch (JsonProcessingException e) {
            throw invalid(provider, json, e);
        }
    }

    private ExternalServiceException invalid(String provider, String json, Exception cause) {
        Log.warnf(cause, "Resposta do %s fora do formato: %s", provider, json);
        String message = "O %s devolveu uma resposta fora do formato esperado. O alerta será tentado de novo."
                .formatted(provider);
        return new ExternalServiceException(message, ErrorCode.LLM_INVALID_RESPONSE, cause);
    }

    private String removeMarkdownFences(String text) {
        return text
                .replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```\\s*$", "")
                .trim();
    }
}
