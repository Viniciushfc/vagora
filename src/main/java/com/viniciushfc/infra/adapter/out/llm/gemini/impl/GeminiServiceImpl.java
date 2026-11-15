package com.viniciushfc.infra.adapter.out.llm.gemini.impl;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import io.quarkus.logging.Log;
import com.google.genai.types.*;
import com.viniciushfc.domain.prompts.PromptsContextsLLM;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.gemini.mapper.GeminiCvMatchMapper;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GeminiServiceImpl {

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
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3.8-flash",
                    prompt,
                    config);

            return cvMatchMapper.toDomain(response);
        } catch (ApiException e) {
            Log.warnf(e, "Gemini recusou a chamada (HTTP %d)", e.code());
            throw switch (e.code()) {
                case 400, 401, 403 -> new ExternalServiceException(
                        "A chave do Gemini foi recusada. Confira a chave no Perfil.", ErrorCode.GEMINI_KEY_REJECTED);
                case 429 -> new ExternalServiceException(
                        "O limite de uso do Gemini foi atingido. O alerta será tentado de novo mais tarde.");
                default -> new ExternalServiceException("O Gemini está indisponível agora. O alerta será tentado de novo.");
            };
        } catch (GenAiIOException e) {
            Log.warn("Falha de comunicação com o Gemini", e);
            throw new ExternalServiceException("Não foi possível falar com o Gemini agora. O alerta será tentado de novo.");
        }
    }
}
