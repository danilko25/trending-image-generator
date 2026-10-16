package com.ai.generation.image.processing;

import com.ai.generation.image.processing.dto.ProcessedImageDto;

import java.util.concurrent.CompletableFuture;

public interface ImageProcessor {
    CompletableFuture<String> process(ProcessedImageDto processedImage);
}
