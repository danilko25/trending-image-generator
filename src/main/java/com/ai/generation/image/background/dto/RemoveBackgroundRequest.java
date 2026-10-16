package com.ai.generation.image.background.dto;

import java.util.UUID;

public record RemoveBackgroundRequest(UUID imageProcessingId, String imagePath) {
}
