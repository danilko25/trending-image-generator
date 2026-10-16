package com.ai.generation.kafka.listener;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.service.GenerationTaskService;
import com.ai.generation.niche.Niche;
import com.ai.generation.trendprocessing.TrendProcessorAIService;
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
public class GenerationTaskListener {
    private final TrendProcessorAIService trendProcessorAIService;
    private final GenerationTaskService generationTaskService;

    @KafkaListener(
            topics = "${task.generation.topic}",
            groupId = "task-generation-group"
    )
    @RetryableTopic(
            attempts = "2",
            backoff = @Backoff(delayExpression = "${spring.kafka.consumer.backoff.ms}"),
            exclude = EntityNotFoundException.class
    )
    public void listen(String taskId) {
        log.info("Start generation task processing: {}", taskId);
        UUID id = UUID.fromString(taskId);
        var task = generationTaskService.findById(id);

        var updated = generationTaskService.transitionStatus(id, GenerationStatus.PENDING, GenerationStatus.FILTERING);
        if (updated == 0) {
            log.warn("Task {} already processed or in wrong state, skipping duplicate delivery", id);
            return;
        }
        Niche niche;
        try {
            niche = trendProcessorAIService.processTrend(task.getRawTrend(), task.isFilteringNeeded());
        } catch (Exception e) {
            generationTaskService.transitionStatus(id, GenerationStatus.FILTERING, GenerationStatus.PENDING);
            throw e;
        }
        generationTaskService.createSubtasks(id, niche.getId());

    }

    @DltHandler
    public void handleDlt(String taskId, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        UUID id = UUID.fromString(taskId);
        log.error("Image generation sent to DLT. TaskId: {}, Error: {}", taskId, exceptionMessage);
        var exception = new RuntimeException("Image generation failed after 2 retries: " + exceptionMessage);
        generationTaskService.fail(id, exception);
    }
}
