package com.ai.generation.generationtask.service;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.GenerationSubtask;
import com.ai.generation.generationtask.entity.GenerationTask;
import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import com.ai.generation.generationtask.repository.GenerationSubtaskRepository;
import com.ai.generation.generationtask.repository.GenerationTaskRepository;
import com.ai.generation.kafka.dto.ImageGenerationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.ai.generation.generationtask.entity.SubtaskStatus.PROMPT_GENERATED;

@Service
@RequiredArgsConstructor
public class GenerationSubtaskService {
    private final GenerationTaskRepository generationTaskRepository;
    private final GenerationSubtaskRepository generationSubtaskRepository;

    private final ApplicationEventPublisher eventPublisher;

    public GenerationSubtask findById(UUID id) {
        return generationSubtaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GenerationSubtask.class, id));
    }

    @Transactional
    public GenerationSubtask createSubtask(UUID taskId, UUID nicheId, UUID styleId) {
        var subtask = new GenerationSubtask();
        subtask.setStatus(SubtaskStatus.PENDING);
        subtask.setGenerationTaskId(taskId);
        subtask.setNicheId(nicheId);
        subtask.setImageStyleId(styleId);
        subtask.setCreatedAt(Instant.now());
        return generationSubtaskRepository.save(subtask);
    }

    @Transactional
    public int transitionStatus(UUID id, SubtaskStatus oldStatus, SubtaskStatus newStatus) {
        return generationSubtaskRepository.transitionStatus(id, oldStatus, newStatus);
    }

    @Transactional
    public void processGeneratedPrompt(UUID subtaskId, UUID imagePromptId) {
        var subtask = findById(subtaskId);
        subtask.setImagePromptId(imagePromptId);
        subtask.setStatus(PROMPT_GENERATED);
        eventPublisher.publishEvent(new ImageGenerationEvent(subtaskId));
    }

    @Transactional
    public void complete(
            UUID subtaskId,
            UUID designId
    ) {
        var subtask = findById(subtaskId);

        subtask.setDesignId(designId);
        subtask.setStatus(SubtaskStatus.COMPLETED);
        subtask.setFinishedAt(Instant.now());

        checkFinishedSubtasks(subtask.getGenerationTaskId());
    }

    private void checkFinishedSubtasks(UUID taskId) {
        var task = generationTaskRepository.findByIdForUpdate(
                taskId
        ).orElseThrow(() -> new EntityNotFoundException(GenerationTask.class, taskId));

        var finishedCount = generationSubtaskRepository
                .countByGenerationTaskIdAndFinishedAtIsNotNull(taskId);

        var totalCount = generationSubtaskRepository
                .countByGenerationTaskId(taskId);

        if (finishedCount == totalCount) {
            task.setStatus(GenerationStatus.COMPLETED);
            task.setFinishedAt(Instant.now());
        }
    }

    @Transactional
    public void fail(UUID subtaskId, Throwable ex) {
        var subtask = findById(subtaskId);
        subtask.setStatus(SubtaskStatus.FAILED);
        subtask.setErrorMessage(ex.getMessage());
        subtask.setFinishedAt(Instant.now());
        checkFinishedSubtasks(subtask.getGenerationTaskId());
    }
}
