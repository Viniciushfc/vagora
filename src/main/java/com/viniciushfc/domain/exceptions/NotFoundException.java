package com.viniciushfc.domain.exceptions;

/**
 * Recurso não existe (ex.: nenhum perfil configurado).
 */
public non-sealed class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
