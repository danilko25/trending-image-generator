package com.ai.generation.imagestyle.dto;

import com.ai.generation.imagestyle.dto.projection.ImageStyleDetailsProjection;
import lombok.Data;

import java.util.UUID;

@Data
public class ImageStyleDetailsDto {
    private UUID id;
    private String name;
    private String systemPrompt;
    private boolean active;

    public ImageStyleDetailsDto(ImageStyleDetailsProjection projection) {
        this.id = projection.getId();
        this.name = projection.getName();
        this.systemPrompt = projection.getSystemPrompt();
        this.active = projection.getActive();
    }
}
