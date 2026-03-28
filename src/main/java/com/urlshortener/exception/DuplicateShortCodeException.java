package com.urlshortener.exception;

public class DuplicateShortCodeException extends RuntimeException {
    public DuplicateShortCodeException(String message) {
        super(message);
    }

    public DuplicateShortCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
