package com.ai.generation.design.generation;

import com.ai.generation.design.generation.dto.GenerateDesignRequest;
import com.ai.generation.design.generation.dto.GenerateDesignResponse;

public interface DesignGeneratorClient {

    String GLOBAL_NEGATIVE_SUFFIX =
            "celebrity likeness, brand logo, copyrighted character, real person face, watermark";

    GenerateDesignResponse generateImage(GenerateDesignRequest request);
}
