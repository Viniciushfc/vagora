package com.viniciushfc.infra.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.viniciushfc.domain.exceptions.ErrorCode;

import java.time.OffsetDateTime;

/**
 * Corpo padrão de toda resposta de erro da API.
 *
 * @param success      sempre {@code false}, facilita o front tratar sem olhar o status
 * @param typeError    categoria estável do erro, para o front decidir o que fazer
 * @param messageError mensagem legível para mostrar ao usuário
 * @param status       status HTTP, repetido no corpo
 * @param path         rota que gerou o erro
 * @param timestamp    quando aconteceu
 * @param errorId      só em erros inesperados: o mesmo id aparece no log do servidor
 * @param errorCode    só quando o erro tem tratamento específico (ver ErrorCode)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success,
                            @JsonProperty("type_error") ErrorType typeError,
                            @JsonProperty("message_error") String messageError,
                            int status,
                            String path,
                            OffsetDateTime timestamp,
                            @JsonProperty("error_id") String errorId,
                            @JsonProperty("error_code") ErrorCode errorCode) {

    public static ErrorResponse of(ErrorType type, String message, int status, String path) {
        return of(type, message, status, path, null);
    }

    public static ErrorResponse of(ErrorType type, String message, int status, String path, ErrorCode code) {
        return new ErrorResponse(false, type, message, status, path, OffsetDateTime.now(), null, code);
    }

    public static ErrorResponse unexpected(String path, String errorId) {
        return new ErrorResponse(false, ErrorType.INTERNAL_ERROR,
                "Erro inesperado no servidor. Informe o código " + errorId + " ao reportar o problema.",
                ErrorType.INTERNAL_ERROR.status, path, OffsetDateTime.now(), errorId, null);
    }
}
