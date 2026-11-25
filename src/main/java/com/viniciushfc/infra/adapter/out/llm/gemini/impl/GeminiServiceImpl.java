package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import io.quarkus.logging.Log;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.LlmService;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;

@ApplicationScoped
public class GeminiServiceImpl implements LlmService {
    static final Duration TIMEOUT = Duration.ofMinutes(5);
    private static final ExecutorService CALLS = Executors.newVirtualThreadPerTaskExecutor();

    private static final Map<String, Object> ANALYSIS_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("resultados", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "id", Map.of("type", "integer"),
                                    "match", Map.of("type", "boolean"),
                                    "score", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                                    "motivos", Map.of("type", "array", "items", Map.of("type", "string"), "minItems", 2, "maxItems", 5)),
                            "required", List.of("id", "match", "score", "motivos")))),
            "required", List.of("resultados"));

    private final GenerateContentConfig analysisConfig = config(PromptsContextsLLM.MatchAnalysisPrompt, "application/json")
            .toBuilder().responseJsonSchema(ANALYSIS_SCHEMA).build();
    private final GenerateContentConfig curriculumConfig = config(PromptsContextsLLM.AtsCurriculumPrompt, "text/plain");
    private final GenerateContentConfig chatConfig = config(PromptsContextsLLM.ChatPrompt, "text/plain");

    private final CvMatchMapper cvMatchMapper;

    private final String model;

    public GeminiServiceImpl(CvMatchMapper cvMatchMapper, @ConfigProperty(name = "vagora.gemini.model", defaultValue = "gemini-3.8-flash-lite") String model) {
        this.cvMatchMapper = cvMatchMapper;
        this.model = model;
    }

    private static GenerateContentConfig config(String systemPrompt, String mimeType) {
        return GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder().thinkingLevel(ThinkingLevel.Known.LOW))
                .responseMimeType(mimeType)
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .build();
    }

    @Override
    public List<CvMatchResult> analyze(String prompt, int jobs, String apiKey, String model) {
        return cvMatchMapper.toDomain("Gemini", call(analysisConfig, prompt, apiKey, model).text(), jobs);
    }

    @Override
    public String adaptCurriculum(String prompt, String apiKey, String model) {
        String text = call(curriculumConfig, prompt, apiKey, model).text();
        if (text == null || text.isBlank()) {
            throw new ExternalServiceException("O Gemini não devolveu o currículo adaptado. O alerta será tentado de novo.");
        }
        return text.strip();
    }

    @Override
    public String chat(String prompt, String apiKey, String model) {
        String text = call(chatConfig, prompt, apiKey, model).text();
        if (text == null || text.isBlank()) {
            throw new ExternalServiceException("O Gemini não respondeu a mensagem. Tente de novo.");
        }
        return text.strip();
    }

    @Override
    public String defaultModel(String apiKey) {
        return model;
    }

    @Override
    public List<String> models(String apiKey) {
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            List<String> names = new ArrayList<>();
            for (Model found : client.models.list(ListModelsConfig.builder().pageSize(1000).build())) {
                if (found.supportedActions().orElse(List.of()).contains("generateContent")) {
                    found.name().map(name -> name.replaceFirst("^models/", "")).ifPresent(names::add);
                }
            }
            return names;
        } catch (RuntimeException e) {
            Log.warnf("Não foi possível listar os modelos do Gemini: %s", e.getMessage());
            return List.of();
        }
    }

    private GenerateContentResponse call(GenerateContentConfig config, String prompt, String apiKey, String chosen) {
        String model = chosen == null ? this.model : chosen;
        Client client = Client.builder().apiKey(apiKey).build();
        long start = System.nanoTime();
        try {
            GenerateContentResponse response = CompletableFuture
                    .supplyAsync(() -> client.models.generateContent(model, prompt, config), CALLS)
                    .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            var usage = response.usageMetadata();
            Log.infof("Gemini respondeu em %.1f s: %s tokens de entrada, %s pensando, %s de resposta (%s)",
                    (System.nanoTime() - start) / 1e9,
                    usage.flatMap(u -> u.promptTokenCount()).orElse(null),
                    usage.flatMap(u -> u.thoughtsTokenCount()).orElse(null),
                    usage.flatMap(u -> u.candidatesTokenCount()).orElse(null),
                    config == analysisConfig ? "análise" : config == chatConfig ? "chat" : "currículo");
            return response;
        } catch (TimeoutException e) {
            Log.warnf("O Gemini não respondeu em %d s", TIMEOUT.toSeconds());
            throw new ExternalServiceException("O Gemini demorou demais para responder. O alerta será tentado de novo.");
        } catch (ExecutionException e) {
            throw translate(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("A análise do Gemini foi interrompida.");
        } finally {
            client.close();
        }
    }

    private static final Pattern RETRY_IN = Pattern.compile("retry in (?:(\\d+)h)?(?:(\\d+)m)?(?:([\\d.]+)s)?");

    static RateLimitedException quotaExceeded(String apiMessage) {
        Duration wait = Duration.ofMinutes(30);
        Matcher m = RETRY_IN.matcher(apiMessage == null ? "" : apiMessage);
        if (m.find() && (m.group(1) != null || m.group(2) != null || m.group(3) != null)) {
            wait = Duration.ofHours(parse(m.group(1))).plusMinutes(parse(m.group(2)))
                    .plusSeconds((long) Math.ceil(m.group(3) == null ? 0 : Double.parseDouble(m.group(3))));
        }
        return RateLimitedException.quota("Gemini", wait);
    }

    private static long parse(String number) {
        return number == null ? 0 : Long.parseLong(number);
    }

    static RuntimeException translate(Throwable cause) {
        if (cause instanceof ApiException e) {
            Log.warnf(e, "Gemini recusou a chamada (HTTP %d)", e.code());
            boolean keyRejected = e.code() == 401 || e.code() == 403
                    || (e.code() == 400 && e.message() != null && e.message().contains("API key"));
            if (keyRejected) {
                return new ExternalServiceException(
                        "A chave do Gemini foi recusada. Confira a chave em Configurações.", ErrorCode.LLM_KEY_REJECTED);
            }
            return switch (e.code()) {
                case 400 -> new ExternalServiceException(
                        "O Gemini recusou o pedido (HTTP 400). Os detalhes estão no terminal do Vagora.");
                case 429 -> quotaExceeded(e.message());
                default -> new ExternalServiceException("O Gemini está indisponível agora. O alerta será tentado de novo.");
            };
        }
        Log.warn("Falha de comunicação com o Gemini", cause);
        return new ExternalServiceException("Não foi possível falar com o Gemini agora. O alerta será tentado de novo.");
    }
}
