package com.ai.generation.image.upscaling;

import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.upscaling.dto.UpscaleRequest;

public interface ImageUpscaleClient {
    ProcessedImageResponse upscale(UpscaleRequest request);
}
