package com.ai.generation.generationtask.service;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.GenerationTask;
import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.repository.GenerationTaskRepository;
import com.ai.generation.imagestyle.ImageStyle;
import com.ai.generation.imagestyle.ImageStyleService;
import com.ai.generation.kafka.dto.ImagePromptGenerationEvent;
import com.ai.generation.niche.NicheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static com.ai.generation.generationtask.entity.GenerationStatus.GENERATING;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskService {
    private final NicheService nicheService;
    private final ImageStyleService imageStyleService;
    private final ApplicationEventPublisher eventPublisher;
    private final GenerationSubtaskService generationSubtaskService;
    private final GenerationTaskRepository generationTaskRepository;

    @Transactional
    public GenerationTask createTask(String rawTrend, boolean filteringNeeded, Set<UUID> styleIds) {
        var task = new GenerationTask();
        task.setRawTrend(rawTrend);
        task.setFilteringNeeded(filteringNeeded);
        task.setStyleIds(styleIds);
        task.setStatus(GenerationStatus.PENDING);
        task.setCreatedAt(Instant.now());
        return generationTaskRepository.save(task);
    }

    public GenerationTask findById(UUID id) {
        return generationTaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GenerationTask.class, id));
    }

    @Transactional
    public int transitionStatus(UUID id, GenerationStatus oldStatus, GenerationStatus newStatus) {
        return generationTaskRepository.transitionStatus(id, oldStatus, newStatus);
    }

    @Transactional
    public void fail(UUID id, Throwable ex) {
        var task = findById(id);
        task.setStatus(GenerationStatus.FAILED);
        task.setErrorMessage(ex.getMessage());
        task.setFinishedAt(Instant.now());
    }

    @Transactional
    public void createSubtasks(UUID taskId, UUID nicheId) {
        log.info("Start creating subtasks for task: {}, niche: {}", taskId, nicheId);
        var task = findById(taskId);
        task.setStatus(GENERATING);

        var niche = nicheService.getById(nicheId);
        var styles = imageStyleService.resolveImageStyles(task.getStyleIds());

        for (ImageStyle style : styles) {
            var subtask = generationSubtaskService.createSubtask(taskId, niche.getId(), style.getId());
            eventPublisher.publishEvent(new ImagePromptGenerationEvent(subtask.getId()));
        }
    }
}
