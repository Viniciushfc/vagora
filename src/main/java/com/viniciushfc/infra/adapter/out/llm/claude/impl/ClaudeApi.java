package com.viniciushfc.infra.adapter.out.llm.claude.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.Headers;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClaudeApi {

    static final Duration TIMEOUT = Duration.ofMinutes(5);

    private static final JsonOutputFormat ANALYSIS_FORMAT = JsonOutputFormat.builder()
            .schema(JsonOutputFormat.Schema.builder()
                    .additionalProperties(CvMatchMapper.ANALYSIS_SCHEMA.entrySet().stream()
                            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> JsonValue.from(entry.getValue()))))
                    .build())
            .build();

    private final CvMatchMapper cvMatchMapper;
    private final String model;

    public ClaudeApi(CvMatchMapper cvMatchMapper,
                     @ConfigProperty(name = "vagora.claude.api-model", defaultValue = "claude-opus-5-5") String model) {
        this.cvMatchMapper = cvMatchMapper;
        this.model = model;
    }

    public List<CvMatchResult> analyze(String prompt, int jobs, String apiKey) {
        return cvMatchMapper.toDomain("Claude", call(PromptsContextsLLM.MatchAnalysisPrompt, prompt, ANALYSIS_FORMAT, apiKey, "análise"), jobs);
    }

    public String adaptCurriculum(String prompt, String apiKey) {
        String text = call(PromptsContextsLLM.AtsCurriculumPrompt, prompt, null, apiKey, "currículo");
        if (text.isBlank()) {
            throw new ExternalServiceException("O Claude não devolveu o currículo adaptado. O alerta será tentado de novo.");
        }
        return text.strip();
    }

    private String call(String systemPrompt, String prompt, JsonOutputFormat format, String apiKey, String task) {
        OutputConfig.Builder output = OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM);
        if (format != null) {
            output.format(format);
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(systemPrompt)
                .outputConfig(output.build())
                .addUserMessage(prompt)
                .build();

        AnthropicClient client = AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(TIMEOUT).build();
        long start = System.nanoTime();
        try {
            Message message = client.messages().create(params);
            if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
                throw new ExternalServiceException("O Claude se recusou a responder este pedido. O alerta será tentado de novo.");
            }
            Log.infof("Claude (API) respondeu em %.1f s: %d tokens de entrada, %d de resposta (%s)",
                    (System.nanoTime() - start) / 1e9, message.usage().inputTokens(), message.usage().outputTokens(), task);
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining());
        } catch (UnauthorizedException | PermissionDeniedException e) {
            Log.warnf("A API do Claude recusou a chave: %s", e.getMessage());
            throw new ExternalServiceException("A chave do Claude foi recusada. Confira a chave em Configurações.", ErrorCode.LLM_KEY_REJECTED);
        } catch (RateLimitException e) {
            throw RateLimitedException.quota("Claude", retryAfter(e.headers()));
        } catch (BadRequestException e) {
            Log.warnf("A API do Claude recusou o pedido: %s", e.getMessage());
            throw new ExternalServiceException("O Claude recusou o pedido. Os detalhes estão no terminal do Vagora.");
        } catch (AnthropicServiceException e) {
            Log.warnf("A API do Claude falhou (HTTP %d): %s", e.statusCode(), e.getMessage());
            throw new ExternalServiceException("O Claude está indisponível agora. O alerta será tentado de novo.");
        } catch (AnthropicIoException e) {
            Log.warnf("Falha de comunicação com a API do Claude: %s", e.getMessage());
            throw new ExternalServiceException("Não foi possível falar com o Claude agora. O alerta será tentado de novo.");
        } finally {
            client.close();
        }
    }

    static Duration retryAfter(Headers headers) {
        try {
            return headers.values("retry-after").stream().findFirst()
                    .map(value -> Duration.ofSeconds((long) Math.ceil(Double.parseDouble(value))))
                    .orElse(Duration.ofMinutes(1));
        } catch (NumberFormatException e) {
            return Duration.ofMinutes(1);
        }
    }
}
