package com.ai.generation.generationtask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class GenerationSubtask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID generationTaskId;

    @Enumerated(EnumType.STRING)
    private SubtaskStatus status;

    private UUID nicheId;

    private UUID imageStyleId;

    private UUID imagePromptId;

    private UUID designId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant createdAt;

    private Instant finishedAt;
}
