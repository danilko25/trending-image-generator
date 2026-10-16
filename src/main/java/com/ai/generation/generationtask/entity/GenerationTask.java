package com.ai.generation.generationtask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@ToString(exclude = "styleIds")
@AllArgsConstructor
@NoArgsConstructor
public class GenerationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String rawTrend;

    private boolean filteringNeeded;

    @ElementCollection
    private Set<UUID> styleIds;

    @Enumerated(EnumType.STRING)
    private GenerationStatus status;

    private Instant createdAt;

    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
