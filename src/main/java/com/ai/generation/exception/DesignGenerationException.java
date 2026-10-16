package com.ai.generation.exception;

public class DesignGenerationException extends RuntimeException {
    public DesignGenerationException(String message) {
        super(message);
    }

    public DesignGenerationException(String message, Throwable ex) {
        super(message, ex);
    }
}
