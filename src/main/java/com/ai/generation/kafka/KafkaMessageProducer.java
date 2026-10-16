package com.ai.generation.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessageProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${task.generation.topic}")
    private String taskGenerationTopic;
    @Value("${image-prompt.generation.topic}")
    private String imagePromptGenerationTopic;
    @Value("${image.generation.topic}")
    private String imageGenerationTopic;

    public void sendSubtaskTaskGeneration(UUID taskId) {
        kafkaTemplate.send(taskGenerationTopic, taskId.toString());
    }

    public void sendImagePromptGeneration(UUID subtaskId) {
        kafkaTemplate.send(imagePromptGenerationTopic, subtaskId.toString());
    }

    public void sendImageGeneration(UUID subtaskId) {
        kafkaTemplate.send(imageGenerationTopic, subtaskId.toString());
    }
}
