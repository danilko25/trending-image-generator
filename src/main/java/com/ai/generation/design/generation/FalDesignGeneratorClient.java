package com.ai.generation.design.generation;

import ai.fal.client.FalClient;
import ai.fal.client.SubscribeOptions;
import com.ai.generation.exception.DesignGenerationException;
import com.ai.generation.design.generation.dto.GenerateDesignRequest;
import com.ai.generation.design.generation.dto.GenerateDesignResponse;
import com.ai.generation.exception.ImagePromptGenerationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FalDesignGeneratorClient implements DesignGeneratorClient {

    private final FalClient falClient;

    @Value("${fal.generation-model-endpoint}")
    private String modelEndpoint;

    @Override
    public GenerateDesignResponse generateImage(GenerateDesignRequest request) {
        try {
            Map<String, Object> input = Map.of(
                    "prompt", buildFinalPrompt(request),
                    "image_size", "square_hd",
                    "num_images", 1,
                    "output_format", "png"
            );

            var result = falClient.subscribe(modelEndpoint,
                    SubscribeOptions.<JsonObject>builder()
                            .input(input)
                            .logs(true)
                            .resultType(JsonObject.class)
                            .build()
            );

            var url = extractImageUrl(result.getData());
            if (url == null) {
                throw new DesignGenerationException("Service returns null object");
            }
            return new GenerateDesignResponse(url, request.promptId());
        } catch (Exception e) {
            throw new DesignGenerationException("Error occurs on image generation, prompt id: " + request.promptId(), e);
        }
    }

    private String buildFinalPrompt(GenerateDesignRequest request) {
        String base = request.promptText() + ", " + request.stylePrompt();
        String avoid = request.negativePrompt() + ", " + GLOBAL_NEGATIVE_SUFFIX;
        return base + ". Avoid: " + avoid;
    }

    private String extractImageUrl(JsonObject result) {
        JsonArray images = result.getAsJsonArray("images");
        if (images == null || images.isEmpty()) {
            throw new DesignGenerationException("fal.ai result contained no images: " + result);
        }
        return images.get(0).getAsJsonObject().get("url").getAsString();
    }
}
