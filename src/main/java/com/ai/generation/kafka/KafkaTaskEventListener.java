package com.ai.generation.kafka;

import com.ai.generation.kafka.dto.ImageGenerationEvent;
import com.ai.generation.kafka.dto.ImagePromptGenerationEvent;
import com.ai.generation.kafka.dto.TaskGenerationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTaskEventListener {
    private final KafkaMessageProducer kafkaMessageProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskGeneration(TaskGenerationEvent event) {
        kafkaMessageProducer.sendSubtaskTaskGeneration(event.taskId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePromptGeneration(ImagePromptGenerationEvent event) {
        kafkaMessageProducer.sendImagePromptGeneration(event.subtaskId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageGeneration(ImageGenerationEvent event) {
        kafkaMessageProducer.sendImageGeneration(event.subtaskId());
    }
}
