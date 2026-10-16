package com.ai.generation.image.background;

import ai.fal.client.FalClient;
import ai.fal.client.SubscribeOptions;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.background.dto.RemoveBackgroundRequest;
import com.ai.generation.exception.DesignGenerationException;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FalRemoveBackgroundClient implements RemoveBackgroundClient {

    private final FalClient falClient;

    @Value("${fal.remove-background-model-endpoint}")
    private String modelEndpoint;

    @Override
    public ProcessedImageResponse removeBackground(RemoveBackgroundRequest request) {

        var input = Map.of(
                "image_url", request.imagePath()
        );

        try {
            var result = falClient.subscribe(modelEndpoint,
                    SubscribeOptions.<JsonObject>builder()
                            .input(input)
                            .logs(true)
                            .resultType(JsonObject.class)
                            .build()
            );

            var url = extractImageUrl(result.getData());
            if (url == null) {
                throw new DesignGenerationException("Background can't be removed");
            }
            return new ProcessedImageResponse(request.imageProcessingId(), url);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private String extractImageUrl(JsonObject result) {
        JsonObject image = result.getAsJsonObject("image");
        if (image == null) {
            throw new DesignGenerationException("fal.ai result contained no images: " + result);
        }
        return image.getAsJsonObject().get("url").getAsString();
    }
}
