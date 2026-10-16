package com.ai.generation.design.generation;

import com.ai.generation.design.DesignService;
import com.ai.generation.design.dto.DesignCreateDto;
import com.ai.generation.design.Design;
import com.ai.generation.design.generation.dto.GenerateDesignRequest;
import com.ai.generation.imageprompt.ImagePromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DesignGeneratorService {

    private final DesignService designService;
    private final ImagePromptService imagePromptService;
    private final DesignGeneratorClient designGeneratorClient;

    public Design createImage(UUID imagePromptId) {
        var prompt = imagePromptService.getWithStyleById(imagePromptId);
        var generatedResult = designGeneratorClient.generateImage(new GenerateDesignRequest(prompt));
        return designService.createDesign(new DesignCreateDto(prompt.getId(), generatedResult.url()));
    }
}
