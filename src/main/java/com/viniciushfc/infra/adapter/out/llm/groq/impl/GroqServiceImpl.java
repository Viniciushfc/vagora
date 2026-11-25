package com.viniciushfc.infra.adapter.out.llm.groq.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.LlmService;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@ApplicationScoped
public class GroqServiceImpl implements LlmService {

    static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    static final Duration TIMEOUT = Duration.ofMinutes(5);
    static final Duration MAX_INLINE_WAIT = Duration.ofMinutes(1);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final Map<String, Object> ANALYSIS_FORMAT = Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                    "name", "analise_vagas",
                    "strict", true,
                    "schema", CvMatchMapper.ANALYSIS_SCHEMA));

    private final CvMatchMapper cvMatchMapper;
    private final ObjectMapper objectMapper;
    private final String name;
    private final String model;
    private final URI endpoint;
    private final Map<String, Object> extras;

    @Inject
    public GroqServiceImpl(CvMatchMapper cvMatchMapper,
                           ObjectMapper objectMapper,
                           @ConfigProperty(name = "vagora.groq.model", defaultValue = "openai/gpt-oss-120b") String model) {
        this(cvMatchMapper, objectMapper, model, ENDPOINT);
    }

    GroqServiceImpl(CvMatchMapper cvMatchMapper, ObjectMapper objectMapper, String model, URI endpoint) {
        this(cvMatchMapper, objectMapper, "Groq", model, endpoint, Map.of("reasoning_effort", "low"));
    }

    private GroqServiceImpl(CvMatchMapper cvMatchMapper, ObjectMapper objectMapper, String name, String model, URI endpoint,
                            Map<String, Object> extras) {
        this.cvMatchMapper = cvMatchMapper;
        this.objectMapper = objectMapper;
        this.name = name;
        this.model = model;
        this.endpoint = endpoint;
        this.extras = extras;
    }

    public static LlmService compatible(CvMatchMapper cvMatchMapper, ObjectMapper objectMapper, String name, String model, URI endpoint) {
        return new GroqServiceImpl(cvMatchMapper, objectMapper, name, model, endpoint, Map.of());
    }

    @Override
    public List<CvMatchResult> analyze(String prompt, int jobs, String apiKey, String model) {
        String text = call(PromptsContextsLLM.MatchAnalysisPrompt, prompt, ANALYSIS_FORMAT, apiKey, model, "análise");
        return cvMatchMapper.toDomain(name, text, jobs);
    }

    @Override
    public String adaptCurriculum(String prompt, String apiKey, String model) {
        String text = call(PromptsContextsLLM.AtsCurriculumPrompt, prompt, null, apiKey, model, "currículo");
        if (text == null || text.isBlank()) {
            throw new ExternalServiceException(name + " não devolveu o currículo adaptado. O alerta será tentado de novo.");
        }
        return text.strip();
    }

    @Override
    public String chat(String prompt, String apiKey, String model) {
        String text = call(PromptsContextsLLM.ChatPrompt, prompt, null, apiKey, model, "chat");
        if (text == null || text.isBlank()) {
            throw new ExternalServiceException(name + " não respondeu a mensagem. Tente de novo.");
        }
        return text.strip();
    }

    @Override
    public String defaultModel(String apiKey) {
        return model;
    }

    @Override
    public List<String> models(String apiKey) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint.toString().replaceFirst("chat/completions$", "models"))).timeout(Duration.ofSeconds(30)).GET();
            if (apiKey != null && !apiKey.isBlank()) {
                request.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Log.warnf("%s não listou os modelos (HTTP %d)", name, response.statusCode());
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            objectMapper.readTree(response.body()).path("data").forEach(found -> ids.add(found.path("id").asText()));
            return ids.stream().filter(id -> !id.isBlank()).sorted().toList();
        } catch (IOException | RuntimeException e) {
            Log.warnf("Não foi possível listar os modelos de %s: %s", name, e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private String call(String systemPrompt, String prompt, Map<String, Object> format, String apiKey, String chosen, String task) {
        long start = System.nanoTime();
        try {
            HttpRequest request = request(body(systemPrompt, prompt, format, chosen == null ? model : chosen), apiKey);
            HttpResponse<String> response = sendWaitingShortLimit(request);
            if (response.statusCode() != 200) {
                throw translate(name, response.statusCode(), retryAfterHeader(response), response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode usage = json.path("usage");
            Log.infof("%s respondeu em %.1f s: %d tokens de entrada, %d de resposta (%s)",
                    name, (System.nanoTime() - start) / 1e9,
                    usage.path("prompt_tokens").asInt(),
                    usage.path("completion_tokens").asInt(),
                    task);
            return json.path("choices").path(0).path("message").path("content").asText(null);
        } catch (HttpTimeoutException e) {
            Log.warnf("%s não respondeu em %d s", name, TIMEOUT.toSeconds());
            throw new ExternalServiceException(name + " demorou demais para responder. O alerta será tentado de novo.");
        } catch (IOException e) {
            Log.warnf("Falha de comunicação com %s: %s", name, e.getMessage());
            throw new ExternalServiceException("Não foi possível falar com " + name + " agora. O alerta será tentado de novo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("A análise de " + name + " foi interrompida.");
        }
    }

    private String body(String systemPrompt, String prompt, Map<String, Object> format, String model) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)));
        body.putAll(extras);
        if (format != null) {
            body.put("response_format", format);
        }
        return objectMapper.writeValueAsString(body);
    }

    private HttpRequest request(String body, String apiKey) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (apiKey != null && !apiKey.isBlank()) {
            request.header("Authorization", "Bearer " + apiKey);
        }
        return request.build();
    }

    private HttpResponse<String> sendWaitingShortLimit(HttpRequest request)
            throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        Duration wait = retryAfter(retryAfterHeader(response));
        if (response.statusCode() != 429 || wait.compareTo(MAX_INLINE_WAIT) > 0) {
            return response;
        }

        Log.infof("%s pediu %d s de espera pelo limite por minuto; tentando de novo", name, wait.toSeconds());
        Thread.sleep(wait.toMillis());
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String retryAfterHeader(HttpResponse<String> response) {
        return response.headers().firstValue("retry-after").orElse(null);
    }

    static RuntimeException translate(String name, int status, String retryAfter, String body) {
        Log.warnf("%s recusou a chamada (HTTP %d): %s", name, status, body);
        return switch (status) {
            case 401, 403 -> new ExternalServiceException(
                    "A chave de " + name + " foi recusada. Confira a chave em Configurações.", ErrorCode.LLM_KEY_REJECTED);
            case 429 -> RateLimitedException.quota(name, retryAfter(retryAfter));
            case 400, 404, 413, 422 -> new ExternalServiceException(
                    name + " recusou o pedido (HTTP " + status + "). Os detalhes estão no terminal do Vagora.");
            default -> new ExternalServiceException(
                    name + " está indisponível agora. O alerta será tentado de novo.");
        };
    }

    private static Duration retryAfter(String header) {
        if (header == null) {
            return Duration.ofMinutes(1);
        }
        try {
            return Duration.ofSeconds((long) Math.ceil(Double.parseDouble(header)));
        } catch (NumberFormatException e) {
            Log.warnf("retry-after do Groq ilegível: %s", header);
            return Duration.ofMinutes(1);
        }
    }
}
