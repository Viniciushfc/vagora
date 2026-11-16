package com.viniciushfc.domain.exceptions;

public non-sealed class BusinessException extends DomainException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, ErrorCode code) {
        super(message, code);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
