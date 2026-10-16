package com.ai.generation.image.background;

import com.ai.generation.design.DesignService;
import com.ai.generation.image.ApiCallLimiter;
import com.ai.generation.image.background.dto.RemoveBackgroundRequest;
import com.ai.generation.image.processing.ImageProcessor;
import com.ai.generation.image.processing.dto.ProcessedImageDto;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
public class RemoveBackgroundProcessor implements ImageProcessor {
    private final DesignService designService;
    private final ApiCallLimiter apiCallLimiter;
    private final ExecutorService virtualThreadExecutor;
    private final RemoveBackgroundClient removeBackgroundClient;

    @Override
    public CompletableFuture<String> process(ProcessedImageDto processedImageDto) {
        return CompletableFuture.supplyAsync(() -> apiCallLimiter.execute(() -> {
                        var design = designService.findById(processedImageDto.designId());
                        return removeBackgroundClient.removeBackground(new RemoveBackgroundRequest(processedImageDto.id(), design.getImagePath()));
                }), virtualThreadExecutor)
                .thenApply(ProcessedImageResponse::url);
    }
}
