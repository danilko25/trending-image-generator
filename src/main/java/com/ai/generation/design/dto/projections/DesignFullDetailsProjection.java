package com.ai.generation.design.dto.projections;

import java.util.UUID;

public interface DesignFullDetailsProjection {
    UUID getId();

    UUID getPromptNicheId();

    String getPromptNicheKeyword();

    UUID getPromptStyleId();

    String getPromptStyleName();

    String getImagePath();

    UUID getPromptId();

    String getPromptTitle();

    String getPromptMainTag();

    String getPromptDescription();

    String getPromptValue();

    String getPromptNegativeValue();

}
