package com.ai.generation.kafka.dto;

import java.util.UUID;

public record ImageGenerationEvent(UUID subtaskId) {
}
