package com.ai.generation.image.processing.dto;

import com.ai.generation.image.processing.entity.ProcessingStatus;
import com.ai.generation.image.processing.entity.ProcessingType;

import java.util.UUID;

public interface ProcessedImageProjection {
    UUID getId();
    ProcessingType getType();
    ProcessingStatus getStatus();
    String getResultUrl();
    String getErrorMessage();
}
