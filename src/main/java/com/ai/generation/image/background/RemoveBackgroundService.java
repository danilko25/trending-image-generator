package com.ai.generation.image.background;

import com.ai.generation.design.DesignService;
import com.ai.generation.design.dto.DesignShortDetailsDto;
import com.ai.generation.image.processing.ProcessedImageService;
import com.ai.generation.image.processing.entity.ProcessingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class RemoveBackgroundService {

    private final DesignService designService;
    private final ProcessedImageService processedImageService;

    @Transactional
    public DesignShortDetailsDto removeBackground(UUID id) {
        log.info("Start processing remove background request for design: {}", id);
        var design = designService.findByIdWithProcessedImages(id);
        processedImageService.startProcessing(design, ProcessingType.REMOVE_BACKGROUND);
        return new DesignShortDetailsDto(design);
    }
}
