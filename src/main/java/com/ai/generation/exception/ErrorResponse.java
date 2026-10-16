package com.ai.generation.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private String code;
    private String message;
    private long timestamp;

    public ErrorResponse(String code, String message, long timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }
}
