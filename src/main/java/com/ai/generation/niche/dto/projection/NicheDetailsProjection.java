package com.ai.generation.niche.dto.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NicheDetailsProjection {

    UUID getId();
    String getKeyword();
    LocalDateTime getCreatedAt();
    LocalDateTime getLastUsedAt();
}
