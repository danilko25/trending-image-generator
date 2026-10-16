package com.ai.generation.exception;

public class ImagePromptGenerationException extends RuntimeException {
    public ImagePromptGenerationException(String message) {
        super(message);
    }

    public ImagePromptGenerationException(String message, Exception e) {
      super(message, e);
    }
}
