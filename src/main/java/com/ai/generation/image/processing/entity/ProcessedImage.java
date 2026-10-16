package com.ai.generation.image.processing.entity;

import com.ai.generation.design.Design;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
public class ProcessedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Design design;

    @Enumerated(EnumType.STRING)
    private ProcessingType type;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private String resultUrl;
    private String errorMessage;
    private Instant createdAt;
}
