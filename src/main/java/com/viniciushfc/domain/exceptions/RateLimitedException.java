package com.viniciushfc.domain.exceptions;

import java.time.Duration;

public class RateLimitedException extends ExternalServiceException {

    private final Duration retryAfter;

    public RateLimitedException(String message, ErrorCode code, Duration retryAfter) {
        super(message, code);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
