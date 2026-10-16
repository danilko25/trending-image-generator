package com.ai.generation.exception;

public class TrendProcessingException extends RuntimeException {
    public TrendProcessingException(String message) {
        super(message);
    }

    public TrendProcessingException(String message, Exception e) {
        super(message, e);
    }
}