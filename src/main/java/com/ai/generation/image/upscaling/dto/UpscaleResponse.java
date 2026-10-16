package com.ai.generation.image.upscaling.dto;

import java.util.UUID;

public record UpscaleResponse(UUID designId, String imagePath) {
}
