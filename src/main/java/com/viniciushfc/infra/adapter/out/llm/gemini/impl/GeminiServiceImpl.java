package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import io.quarkus.logging.Log;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.gemini.mapper.GeminiCvMatchMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class GeminiServiceImpl {

    private static final String MODEL = "gemini-3.8-flash";
    // o SDK só limita o tempo de conexão, não a espera da resposta: sem isso uma chamada travada
    // prende o monitoramento para sempre. shortcut: valor fixo; respostas normais levam segundos.
    static final Duration TIMEOUT = Duration.ofSeconds(90);
    private static final ExecutorService CALLS = Executors.newVirtualThreadPerTaskExecutor();

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
        Client client = Client.builder().apiKey(apiKey).build();
        try {
            GenerateContentResponse response = CompletableFuture
                    .supplyAsync(() -> client.models.generateContent(MODEL, prompt, config), CALLS)
                    .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return cvMatchMapper.toDomain(response);
        } catch (TimeoutException e) {
            Log.warnf("O Gemini não respondeu em %d s", TIMEOUT.toSeconds());
            throw new ExternalServiceException("O Gemini demorou demais para responder. O alerta será tentado de novo.");
        } catch (ExecutionException e) {
            throw translate(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("A análise do Gemini foi interrompida.");
        } finally {
            client.close(); // fecha a conexão, inclusive a que ficou pendurada no timeout
        }
    }

    private RuntimeException translate(Throwable cause) {
        if (cause instanceof ApiException e) {
            Log.warnf(e, "Gemini recusou a chamada (HTTP %d)", e.code());
            return switch (e.code()) {
                case 400, 401, 403 -> new ExternalServiceException(
                        "A chave do Gemini foi recusada. Confira a chave no Perfil.", ErrorCode.GEMINI_KEY_REJECTED);
                case 429 -> new ExternalServiceException(
                        "O limite de uso do Gemini foi atingido. O alerta será tentado de novo mais tarde.");
                default -> new ExternalServiceException("O Gemini está indisponível agora. O alerta será tentado de novo.");
            };
        }
        Log.warn("Falha de comunicação com o Gemini", cause);
        return new ExternalServiceException("Não foi possível falar com o Gemini agora. O alerta será tentado de novo.");
    }
}
