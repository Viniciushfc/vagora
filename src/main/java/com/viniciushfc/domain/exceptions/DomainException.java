package com.viniciushfc.domain.exceptions;

/**
 * Base das exceptions de negócio, fechada nestas cinco categorias: quem traduz para HTTP
 * (DomainExceptionMapper) é obrigado pelo compilador a tratar todas. Para um erro mais
 * específico, estenda uma categoria ou passe um {@link ErrorCode}.
 */
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

    /** Código para tratamento específico, ou {@code null} quando a categoria basta. */
    public ErrorCode code() {
        return code;
    }
}
