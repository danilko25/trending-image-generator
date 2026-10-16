package com.ai.generation.generationtask.service;

import com.ai.generation.exception.EntityNotFoundException;
import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.entity.GenerationTask;
import com.ai.generation.generationtask.repository.GenerationTaskRepository;
import com.ai.generation.imagestyle.ImageStyleService;
import com.ai.generation.niche.NicheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;
import java.util.UUID;

import static com.ai.generation.generationtask.entity.GenerationStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceTest {

    @Mock
    private NicheService nicheService;

    @Mock
    private ImageStyleService imageStyleService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GenerationSubtaskService generationSubtaskService;

    @Mock
    private GenerationTaskRepository generationTaskRepository;

    @InjectMocks
    private GenerationTaskService generationTaskService;


    @Test
    void shouldCreateTask() {
        var rawTrend = "cute cat";
        var filteringNeeded = true;
        var styleIds = Set.of(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        var savedTask = new GenerationTask();
        savedTask.setId(UUID.randomUUID());

        when(generationTaskRepository.save(any(GenerationTask.class)))
                .thenReturn(savedTask);

        // when
        var result = generationTaskService.createTask(
                rawTrend,
                filteringNeeded,
                styleIds
        );

        // then
        assertThat(result).isSameAs(savedTask);

        var captor = ArgumentCaptor.forClass(GenerationTask.class);

        verify(generationTaskRepository).save(captor.capture());

        var task = captor.getValue();

        assertThat(task.getRawTrend()).isEqualTo(rawTrend);
        assertThat(task.isFilteringNeeded()).isTrue();
        assertThat(task.getStyleIds()).isEqualTo(styleIds);
        assertThat(task.getStatus()).isEqualTo(PENDING);
        assertThat(task.getCreatedAt()).isNotNull();
    }


    @Test
    void shouldFindTaskById() {
        // given
        var taskId = UUID.randomUUID();

        var task = new GenerationTask();
        task.setId(taskId);

        when(generationTaskRepository.findById(taskId))
                .thenReturn(java.util.Optional.of(task));

        // when
        var result = generationTaskService.findById(taskId);

        // then
        assertThat(result).isSameAs(task);

        verify(generationTaskRepository).findById(taskId);
    }


    @Test
    void shouldThrowExceptionWhenTaskNotFound() {
        // given
        var taskId = UUID.randomUUID();

        when(generationTaskRepository.findById(taskId))
                .thenReturn(java.util.Optional.empty());

        // when / then
        assertThatThrownBy(() -> generationTaskService.findById(taskId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(generationTaskRepository).findById(taskId);
    }

    @Test
    void shouldFailTask() {
        // given
        var taskId = UUID.randomUUID();
        var exception = new RuntimeException("AI service failed");

        var task = new GenerationTask();
        task.setId(taskId);
        task.setStatus(GenerationStatus.FILTERING);

        when(generationTaskRepository.findById(taskId))
                .thenReturn(java.util.Optional.of(task));

        // when
        generationTaskService.fail(taskId, exception);

        // then
        assertThat(task.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("AI service failed");
        assertThat(task.getFinishedAt()).isNotNull();

        verify(generationTaskRepository).findById(taskId);
    }
}