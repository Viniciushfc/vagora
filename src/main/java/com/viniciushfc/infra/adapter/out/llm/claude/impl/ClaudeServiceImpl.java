package com.viniciushfc.infra.adapter.out.llm.claude.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.claude.ClaudeCli;
import com.viniciushfc.infra.adapter.out.llm.LlmService;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class ClaudeServiceImpl implements LlmService {

    static final Duration TIMEOUT = Duration.ofMinutes(5);
    static final Duration QUOTA_WAIT = Duration.ofHours(1);

    private final CvMatchMapper cvMatchMapper;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String analysisSchema;

    public ClaudeServiceImpl(CvMatchMapper cvMatchMapper,
                             ObjectMapper objectMapper,
                             @ConfigProperty(name = "vagora.claude.model", defaultValue = "sonnet") String model)
            throws JsonProcessingException {
        this.cvMatchMapper = cvMatchMapper;
        this.objectMapper = objectMapper;
        this.model = model;
        this.analysisSchema = objectMapper.writeValueAsString(CvMatchMapper.ANALYSIS_SCHEMA);
    }

    @Override
    public List<CvMatchResult> analyze(String prompt, int jobs, String apiKey) {
        JsonNode json = call(PromptsContextsLLM.MatchAnalysisPrompt, prompt, analysisSchema, "análise");
        JsonNode output = json.path("structured_output");
        return cvMatchMapper.toDomain("Claude", output.isMissingNode() ? null : output.toString(), jobs);
    }

    @Override
    public String adaptCurriculum(String prompt, String apiKey) {
        String text = call(PromptsContextsLLM.AtsCurriculumPrompt, prompt, null, "currículo").path("result").asText(null);
        if (text == null || text.isBlank()) {
            throw new ExternalServiceException("O Claude não devolveu o currículo adaptado. O alerta será tentado de novo.");
        }
        return text.strip();
    }

    private JsonNode call(String systemPrompt, String prompt, String schema, String task) {
        String executable = ClaudeCli.executable();
        if (executable == null) {
            throw new ExternalServiceException("O Claude Code não está instalado neste computador. Veja em Configurações.",
                    ErrorCode.CLAUDE_NOT_INSTALLED);
        }
        List<String> command = new ArrayList<>(List.of(executable, "-p",
                "--output-format", "json",
                "--model", model,
                "--effort", "low",
                "--system-prompt", systemPrompt,
                "--tools", "",
                "--setting-sources", "",
                "--strict-mcp-config",
                "--disable-slash-commands",
                "--no-session-persistence"));
        if (schema != null) {
            command.addAll(List.of("--json-schema", schema));
        }

        long start = System.nanoTime();
        try {
            ClaudeCli.Result result = ClaudeCli.run(command, prompt, TIMEOUT);
            JsonNode json = objectMapper.readTree(result.output());
            if (json.path("is_error").asBoolean(result.exitCode() != 0)) {
                throw translate(json.path("api_error_status").asInt(0), json.path("result").asText(""));
            }
            JsonNode usage = json.path("usage");
            Log.infof("Claude respondeu em %.1f s: %d tokens de entrada, %d de resposta (%s)",
                    (System.nanoTime() - start) / 1e9,
                    usage.path("input_tokens").asInt() + usage.path("cache_read_input_tokens").asInt()
                            + usage.path("cache_creation_input_tokens").asInt(),
                    usage.path("output_tokens").asInt(),
                    task);
            return json;
        } catch (TimeoutException e) {
            Log.warnf("O Claude não respondeu em %d s", TIMEOUT.toSeconds());
            throw new ExternalServiceException("O Claude demorou demais para responder. O alerta será tentado de novo.");
        } catch (IOException e) {
            Log.warn("Falha ao rodar o Claude Code", e);
            throw new ExternalServiceException("Não foi possível rodar o Claude agora. O alerta será tentado de novo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("A análise do Claude foi interrompida.");
        }
    }

    static RuntimeException translate(int status, String message) {
        Log.warnf("Claude recusou a chamada (HTTP %d): %s", status, message);
        String text = message.toLowerCase();
        if (status == 401 || text.contains("not logged in") || text.contains("/login")) {
            return new ExternalServiceException(
                    "O Claude saiu da conta neste computador. Entre de novo em Configurações.", ErrorCode.CLAUDE_NOT_CONNECTED);
        }
        if (status == 429 || text.contains("limit")) {
            return RateLimitedException.quota("Claude", QUOTA_WAIT);
        }
        return new ExternalServiceException("O Claude está indisponível agora. O alerta será tentado de novo.");
    }
}
