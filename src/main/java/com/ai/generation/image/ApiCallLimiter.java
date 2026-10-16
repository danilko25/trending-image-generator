package com.ai.generation.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ApiCallLimiter {
    private final Semaphore apiConnectionLimiter;

    public <T> T execute(Supplier<T> call) {
        try {
            apiConnectionLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring API connection", e);
        }
        try {
            return call.get();
        } finally {
            apiConnectionLimiter.release();
        }
    }
}
