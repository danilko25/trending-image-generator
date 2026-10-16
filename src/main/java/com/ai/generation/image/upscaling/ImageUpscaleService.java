package com.ai.generation.image.upscaling;

import com.ai.generation.design.DesignService;
import com.ai.generation.design.dto.DesignShortDetailsDto;
import com.ai.generation.image.processing.ProcessedImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.ai.generation.image.processing.entity.ProcessingType.UPSCALE;
import static com.ai.generation.image.processing.entity.ProcessingType.UPSCALE_REMOVE_BACKGROUND;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ImageUpscaleService {
    private final DesignService designService;
    private final ProcessedImageService processedImageService;

    public DesignShortDetailsDto upscaleOriginalImage(UUID designId) {
        log.info("Start processing upscale request for design: {}", designId);
        var design = designService.findByIdWithProcessedImages(designId);
        processedImageService.startProcessing(design, UPSCALE);
        return new DesignShortDetailsDto(design);
    }

    public DesignShortDetailsDto upscaleRemovedBackgroundImage(UUID designId) {
        log.info("Start processing upscale removed background request for design: {}", designId);
        var design = designService.findByIdWithProcessedImages(designId);
        processedImageService.startProcessing(design, UPSCALE_REMOVE_BACKGROUND);
        return new DesignShortDetailsDto(design);
    }


}
