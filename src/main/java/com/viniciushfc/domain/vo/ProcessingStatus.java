package com.viniciushfc.domain.vo;

import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;

import java.time.LocalDateTime;

/**
 * Situação do monitoramento do perfil.
 *
 * @param lastCheckAt  última vez que o ciclo rodou
 * @param currentAlert alerta sendo analisado agora
 * @param lastError    último erro (limpo quando um alerta dá certo)
 * @param lastErrorCode código do último erro, quando ele tem tratamento específico
 * @param lastErrorAt  quando o último erro aconteceu
 * @param nextAlertAt  antes disso nenhum alerta novo é aberto (ritmo de segurança do LinkedIn)
 * @param queuedAlerts alertas não lidos esperando a vez
 */
public record ProcessingStatus(LocalDateTime lastCheckAt,
                               String currentAlert,
                               String lastError,
                               ErrorCode lastErrorCode,
                               LocalDateTime lastErrorAt,
                               LocalDateTime nextAlertAt,
                               int queuedAlerts) {

    public static ProcessingStatus empty() {
        return new ProcessingStatus(null, null, null, null, null, null, 0);
    }

    public boolean isWaiting(LocalDateTime now) {
        return nextAlertAt != null && now.isBefore(nextAlertAt);
    }

    public ProcessingStatus analysing(String alert, int queued) {
        return new ProcessingStatus(lastCheckAt, alert, lastError, lastErrorCode, lastErrorAt, nextAlertAt, queued);
    }

    public ProcessingStatus succeeded() {
        return new ProcessingStatus(lastCheckAt, null, null, null, null, nextAlertAt, queuedAlerts);
    }

    public ProcessingStatus failed(Exception error) {
        ErrorCode code = error instanceof DomainException domain ? domain.code() : null;
        return new ProcessingStatus(lastCheckAt, null, error.getMessage(), code, LocalDateTime.now(), nextAlertAt, queuedAlerts);
    }

    public ProcessingStatus waitUntil(LocalDateTime next) {
        return new ProcessingStatus(lastCheckAt, currentAlert, lastError, lastErrorCode, lastErrorAt, next, queuedAlerts);
    }

    public ProcessingStatus queued(int queued) {
        return new ProcessingStatus(lastCheckAt, currentAlert, lastError, lastErrorCode, lastErrorAt, nextAlertAt, queued);
    }

    public ProcessingStatus checked() {
        return new ProcessingStatus(LocalDateTime.now(), null, lastError, lastErrorCode, lastErrorAt, nextAlertAt, queuedAlerts);
    }
}
