package com.viniciushfc.domain.exceptions;

public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ValidationException, ConflictException, BusinessException, ExternalServiceException {

    private final ErrorCode code;

    protected DomainException(String message) {
        this(message, (ErrorCode) null);
    }

    protected DomainException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
        this.code = null;
    }

    public ErrorCode code() {
        return code;
    }
}
