package com.ai.generation.niche.dto;

import com.ai.generation.niche.dto.projection.NicheDetailsProjection;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NicheDetailsDto {
    UUID id;
    String keyword;
    LocalDateTime createdAt;
    LocalDateTime lastUsedAt;

    public NicheDetailsDto(NicheDetailsProjection projection) {
        this.id = projection.getId();
        this.keyword = projection.getKeyword();
        this.createdAt = projection.getCreatedAt();
        this.lastUsedAt = projection.getLastUsedAt();
    }
}
