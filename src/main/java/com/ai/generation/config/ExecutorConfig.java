package com.ai.generation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


@Configuration
public class ExecutorConfig {

    @Value("${parallel.connections.count}")
    private int parallelConnectionsCount;

    @Bean ("apiConnectionLimiter")
    public Semaphore apiConnectionLimiter() {
        return new Semaphore(parallelConnectionsCount);
    }

    @Bean("virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

