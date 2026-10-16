package com.ai.generation.imagestyle.dto;

import com.ai.generation.imagestyle.ImageStyle;
import lombok.Data;

import java.util.UUID;

@Data
public class ImageStyleResponse {
    private UUID id;
    private String name;
    private String systemPrompt;
    private boolean active;

    public ImageStyleResponse(ImageStyle imageStyle) {
        this.id = imageStyle.getId();
        this.name = imageStyle.getName();
        this.systemPrompt = imageStyle.getSystemPrompt();
        this.active = imageStyle.isActive();
    }
}
