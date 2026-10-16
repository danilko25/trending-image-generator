package com.ai.generation.image.processing;

import com.ai.generation.image.background.RemoveBackgroundProcessor;
import com.ai.generation.image.processing.entity.ProcessingType;
import com.ai.generation.image.upscaling.UpscaleRemovedBackgroundProcessor;
import com.ai.generation.image.upscaling.UpscaleProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageProcessorFactory {
    private final RemoveBackgroundProcessor removeBackgroundProcessor;
    private final UpscaleProcessor upscaleProcessor;
    private final UpscaleRemovedBackgroundProcessor upscaleRemovedBackgroundProcessor;

    public ImageProcessor getProcessor(ProcessingType type) {
        return switch (type) {
            case REMOVE_BACKGROUND -> removeBackgroundProcessor;
            case UPSCALE -> upscaleProcessor;
            case UPSCALE_REMOVE_BACKGROUND -> upscaleRemovedBackgroundProcessor;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}