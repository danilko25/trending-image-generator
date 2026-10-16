package com.ai.generation.image.processing;

import com.ai.generation.image.processing.dto.ProcessedImageDto;
import com.ai.generation.image.processing.entity.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessingExecutor {
    private final ImageProcessorFactory processorFactory;
    private final ProcessedImageService processedImageService;

    public void execute(UUID imageProcessingId) {
        try {
            log.info("Start async processing for design image: {}", imageProcessingId);
            processedImageService.setStatus(imageProcessingId, ProcessingStatus.PROCESSING);

            var processedImage = processedImageService.findById(imageProcessingId);
            var processor = processorFactory.getProcessor(processedImage.getType());
            processor.process(new ProcessedImageDto(processedImage))
                    .thenAccept(url -> processedImageService.complete(processedImage.getId(), url))
                    .exceptionally(ex -> {
                        processedImageService.fail(processedImage.getId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            processedImageService.fail(imageProcessingId, e);
        }
    }
}
