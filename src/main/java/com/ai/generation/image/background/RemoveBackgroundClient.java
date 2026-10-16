package com.ai.generation.image.background;

import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.background.dto.RemoveBackgroundRequest;

public interface RemoveBackgroundClient {
    ProcessedImageResponse removeBackground(RemoveBackgroundRequest request);
}
