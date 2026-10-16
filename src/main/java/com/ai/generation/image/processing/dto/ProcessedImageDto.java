package com.ai.generation.image.processing.dto;

import com.ai.generation.image.processing.entity.ProcessedImage;

import java.util.UUID;

public record ProcessedImageDto(UUID id, UUID designId) {

    public ProcessedImageDto(ProcessedImage processedImage) {
        this(processedImage.getId(), processedImage.getDesign().getId());
    }
}
