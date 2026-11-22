package com.viniciushfc.infra.adapter.in.rest.dto;

public enum ErrorType {
    NOT_FOUND(404),
    VALIDATION_ERROR(400),
    CONFLICT(409),
    BUSINESS_RULE(422),
    EXTERNAL_SERVICE_ERROR(502),
    HTTP_ERROR(500),
    INTERNAL_ERROR(500);

    public final int status;

    ErrorType(int status) {
        this.status = status;
    }
}
