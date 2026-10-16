package com.ai.generation.image.upscaling;

import com.ai.generation.image.ApiCallLimiter;
import com.ai.generation.image.processing.ImageProcessor;
import com.ai.generation.image.processing.ProcessedImageService;
import com.ai.generation.image.processing.dto.ProcessedImageDto;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.processing.entity.ProcessingType;
import com.ai.generation.image.upscaling.dto.UpscaleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
public class UpscaleRemovedBackgroundProcessor implements ImageProcessor {
    private final ApiCallLimiter apiCallLimiter;
    private final ExecutorService virtualThreadExecutor;
    private final ImageUpscaleClient imageUpscaleClient;
    private final ProcessedImageService processedImageService;

    @Override
    public CompletableFuture<String> process(ProcessedImageDto processedImageDto) {
        return CompletableFuture.supplyAsync(() -> apiCallLimiter.execute(() -> {
                        var removedBackgroundImage = processedImageService.findByTypeForDesign(processedImageDto.designId(), ProcessingType.REMOVE_BACKGROUND);
                        return imageUpscaleClient.upscale(new UpscaleRequest(processedImageDto.id(), removedBackgroundImage.getResultUrl()));
                }), virtualThreadExecutor)
                .thenApply(ProcessedImageResponse::url);
    }
}
