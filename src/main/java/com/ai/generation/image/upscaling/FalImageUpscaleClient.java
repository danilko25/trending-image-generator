package com.ai.generation.image.upscaling;

import ai.fal.client.FalClient;
import ai.fal.client.SubscribeOptions;
import com.ai.generation.image.processing.dto.ProcessedImageResponse;
import com.ai.generation.image.upscaling.dto.UpscaleRequest;
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
public class FalImageUpscaleClient implements ImageUpscaleClient {

    private final FalClient falClient;

    @Value("${fal.upscale-model-endpoint}")
    private String modelEndpoint;

    @Override
    public ProcessedImageResponse upscale(UpscaleRequest request) {
        var input = Map.of(
                "scale", 2,
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
                throw new DesignGenerationException("Image can't be upscaled");
            }
            return new ProcessedImageResponse(request.designId(), url);
        } catch (Exception e) {
            log.error("Error on image upscaling", e);
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
