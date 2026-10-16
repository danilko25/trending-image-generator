package com.ai.generation.imagestyle.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImageStyleCreateUpdateDto {
    @Size(min = 1, max = 100)
    private String name;
    @Size(min = 1, max = 2000)
    private String systemPrompt;
    private boolean active;
}
