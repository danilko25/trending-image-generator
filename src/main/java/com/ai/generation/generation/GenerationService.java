package com.ai.generation.generation;

import com.ai.generation.generationtask.service.GenerationTaskService;
import com.ai.generation.kafka.dto.TaskGenerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {
    private final GenerationTaskService generationTaskService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void startGeneration(GenerationRequest request) {
        var task = generationTaskService.createTask(
                request.getRawTrend(),
                request.isFilter(),
                request.getStyleIds()
        );
        eventPublisher.publishEvent(new TaskGenerationEvent(task.getId()));
    }
}
