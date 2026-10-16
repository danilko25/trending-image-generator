package com.ai.generation.imagestyle.dto.projection;

import java.util.UUID;

public interface ImageStyleDetailsProjection {
    UUID getId();
    String getName();
    String getSystemPrompt();
    Boolean getActive();
}
