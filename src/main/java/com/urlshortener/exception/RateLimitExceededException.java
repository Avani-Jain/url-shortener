package com.urlshortener.exception;

public class RateLimitExceededException extends RuntimeException {
    private final Long retryAfterSeconds;

    public RateLimitExceededException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}