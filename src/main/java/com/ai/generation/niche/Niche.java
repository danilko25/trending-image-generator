package com.ai.generation.niche;

import jakarta.persistence.Entity;
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
public class Niche {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String keyword;
    private Instant createdAt;
    private Instant lastUsedAt;

    public Niche(String keyword) {
        this.keyword = keyword;
        this.createdAt = Instant.now();
        this.lastUsedAt = null;
    }
}
