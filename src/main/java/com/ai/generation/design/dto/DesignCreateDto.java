package com.ai.generation.design.dto;

import java.util.UUID;

public record DesignCreateDto (UUID promptId, String url) {
}
