package com.ai.generation.image.upscaling;

import com.ai.generation.design.DesignService;
import com.ai.generation.image.ApiCallLimiter;
import com.ai.generation.image.processing.ImageProcessor;
import com.ai.generation.image.processing.dto.ProcessedImageDto;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.upscaling.dto.UpscaleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
public class UpscaleProcessor implements ImageProcessor {
    private final DesignService designService;
    private final ApiCallLimiter apiCallLimiter;
    private final ExecutorService virtualThreadExecutor;
    private final ImageUpscaleClient imageUpscaleClient;

    @Override
    public CompletableFuture<String> process(ProcessedImageDto processedImageDto) {
        return CompletableFuture.supplyAsync(() -> apiCallLimiter.execute(() -> {
                        var design = designService.findById(processedImageDto.designId());
                        return imageUpscaleClient.upscale(new UpscaleRequest(processedImageDto.id(), design.getImagePath()));
                }), virtualThreadExecutor)
                .thenApply(ProcessedImageResponse::url);
    }
}
