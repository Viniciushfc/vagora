package com.viniciushfc.domain.exceptions;

/**
 * Dado de entrada inválido (ex.: currículo grande demais, pasta inexistente, senha errada).
 */
public non-sealed class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
