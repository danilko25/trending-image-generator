package com.ai.generation.kafka.listener;

import com.ai.generation.design.Design;
import com.ai.generation.design.generation.DesignGeneratorService;
import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import com.ai.generation.generationtask.service.GenerationSubtaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerationListener {
    private final DesignGeneratorService designGeneratorService;
    private final GenerationSubtaskService generationSubtaskService;

    @KafkaListener(
            topics = "${image.generation.topic}",
            groupId = "image-generation-group"
    )
    @RetryableTopic(
            attempts = "2",
            backoff = @Backoff(delayExpression = "${spring.kafka.consumer.backoff.ms}"),
            exclude = EntityNotFoundException.class
    )
    public void listen(String subtaskId) {
        log.info("Start image generation: {}", subtaskId);
        UUID id = UUID.fromString(subtaskId);

        var updated = generationSubtaskService.transitionStatus(id, SubtaskStatus.PROMPT_GENERATED, SubtaskStatus.IMAGE_GENERATING);
        if (updated == 0) {
            log.warn("Subtask {} already processed or in wrong state, skipping duplicate delivery", id);
            return;
        }
        Design design;
        try {
            var subtask = generationSubtaskService.findById(id);
            design = designGeneratorService.createImage(subtask.getImagePromptId());
        } catch (Exception e) {
            generationSubtaskService.transitionStatus(id, SubtaskStatus.IMAGE_GENERATING, SubtaskStatus.PROMPT_GENERATED);
            throw e;
        }
        generationSubtaskService.complete(id, design.getId());
    }

    @DltHandler
    public void handleDlt(String subtaskId, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        UUID id = UUID.fromString(subtaskId);
        log.error("Image generation sent to DLT. TaskId: {}, Error: {}", id, exceptionMessage);
        var exception = new RuntimeException("Image generation failed after 2 retries: " + exceptionMessage);
        generationSubtaskService.fail(id, exception);
    }
}
