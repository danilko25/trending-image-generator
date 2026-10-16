package com.ai.generation.image.processing.dto;

import java.util.UUID;

public record ProcessedImageResponse(UUID processedImageId, String url) {
}
