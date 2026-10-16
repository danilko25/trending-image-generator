package com.ai.generation.image.upscaling.dto;

import java.util.UUID;

public record UpscaleRequest (UUID designId, String imagePath) {
}
