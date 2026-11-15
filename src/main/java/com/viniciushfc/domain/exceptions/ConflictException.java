package com.viniciushfc.domain.exceptions;

/**
 * Operação conflita com o estado atual (ex.: criar um segundo perfil na instalação).
 */
public non-sealed class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
