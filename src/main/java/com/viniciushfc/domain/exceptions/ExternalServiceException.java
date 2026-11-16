package com.viniciushfc.domain.exceptions;

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
