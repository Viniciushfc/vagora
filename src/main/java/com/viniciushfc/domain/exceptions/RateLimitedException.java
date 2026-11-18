package com.viniciushfc.domain.exceptions;

import java.time.Duration;

public class RateLimitedException extends ExternalServiceException {

    private final Duration retryAfter;

    public RateLimitedException(String message, ErrorCode code, Duration retryAfter) {
        super(message, code);
        this.retryAfter = retryAfter;
    }

    public static RateLimitedException quota(String provider, Duration wait) {
        String message = "O limite de uso do %s foi atingido. A análise volta a funcionar em cerca de %s."
                .formatted(provider, humanize(wait));
        return new RateLimitedException(message, ErrorCode.LLM_QUOTA_EXCEEDED, wait);
    }

    private static String humanize(Duration wait) {
        long hours = wait.toHours();
        long minutes = wait.toMinutesPart() + (wait.toSecondsPart() > 0 ? 1 : 0);
        if (hours == 0) {
            return Math.max(1, minutes) + " min";
        }
        return hours + " h" + (minutes > 0 ? " " + minutes + " min" : "");
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
