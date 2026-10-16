package com.ai.generation.design.generation.dto;

import com.ai.generation.imageprompt.ImagePrompt;

import java.util.UUID;

public record GenerateDesignRequest(UUID promptId, String promptText, String stylePrompt, String negativePrompt) {

    public GenerateDesignRequest(ImagePrompt imagePrompt) {
        this(imagePrompt.getId(), imagePrompt.getValue(), imagePrompt.getStyle().getSystemPrompt(), imagePrompt.getNegativeValue());
    }
}
