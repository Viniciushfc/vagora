package com.viniciushfc.domain.exceptions;

/**
 * Falha em serviço externo (email, Gemini, scraper do LinkedIn).
 */
public non-sealed class ExternalServiceException extends DomainException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, ErrorCode code) {
        super(message, code);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
