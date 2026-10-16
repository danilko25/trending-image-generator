package com.ai.generation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EventPublisherConfig {

    @Bean
    public ApplicationEventPublisher eventPublisher(ApplicationEventPublisher publisher) {
        return event -> {
            log.info("Publishing event: {}", event);
            publisher.publishEvent(event);
        };
    }
}
