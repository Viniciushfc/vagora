package com.viniciushfc.domain.vo;

import com.viniciushfc.domain.exceptions.DomainException;
import com.viniciushfc.domain.exceptions.ErrorCode;

import java.time.LocalDateTime;

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
        boolean domain = error instanceof DomainException;
        String message = domain ? error.getMessage() : "Erro inesperado ao analisar o alerta. Os detalhes estão no log do Vagora.";
        ErrorCode code = domain ? ((DomainException) error).code() : null;
        return new ProcessingStatus(lastCheckAt, null, message, code, LocalDateTime.now(), nextAlertAt, queuedAlerts);
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
