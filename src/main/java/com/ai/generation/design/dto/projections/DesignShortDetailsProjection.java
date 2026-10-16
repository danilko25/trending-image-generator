package com.ai.generation.design.dto.projections;

import java.util.UUID;

public interface DesignShortDetailsProjection {
    UUID getId();

    String getImagePath();

    UUID getPromptId();

    String getPromptTitle();

    String getPromptMainTag();

    String getPromptDescription();
}
