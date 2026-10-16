package com.ai.generation.kafka.listener;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import com.ai.generation.generationtask.service.GenerationSubtaskService;
import com.ai.generation.imageprompt.ImagePrompt;
import com.ai.generation.imageprompt.ImagePromptService;
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
public class ImagePromptGenerationListener {
    private final ImagePromptService imagePromptService;
    private final GenerationSubtaskService generationSubtaskService;

    @KafkaListener(
            topics = "${image-prompt.generation.topic}",
            groupId = "image-prompt-generation-group"
    )
    @RetryableTopic(
            attempts = "2",
            backoff = @Backoff(delayExpression = "${spring.kafka.consumer.backoff.ms}"),
            exclude = EntityNotFoundException.class
    )
    public void listen(String subtaskId) {
        log.info("Start prompt generation: {}", subtaskId);
        UUID id = UUID.fromString(subtaskId);

        var updated = generationSubtaskService.transitionStatus(id, SubtaskStatus.PENDING, SubtaskStatus.PROMPT_GENERATING);
        if (updated == 0) {
            log.warn("Subtask {} already processed or in wrong state, skipping duplicate delivery", id);
            return;
        }
        ImagePrompt imagePrompt;
        try {
            var subtask = generationSubtaskService.findById(id);
            imagePrompt = imagePromptService.generateImagePrompt(subtask.getImageStyleId(), subtask.getNicheId());
        } catch (Exception e) {
            generationSubtaskService.transitionStatus(id, SubtaskStatus.PROMPT_GENERATING, SubtaskStatus.PENDING);
            throw e;
        }
        generationSubtaskService.processGeneratedPrompt(id, imagePrompt.getId());
    }

    @DltHandler
    public void handleDlt(String subtaskId, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        UUID id = UUID.fromString(subtaskId);
        log.error("Image generation sent to DLT. TaskId: {}, Error: {}", id, exceptionMessage);
        var exception = new RuntimeException("Image generation failed after 2 retries: " + exceptionMessage);
        generationSubtaskService.fail(id, exception);
    }
}
