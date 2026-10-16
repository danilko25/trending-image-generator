package com.ai.generation.generation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Data
@ToString
public class GenerationRequest {
    @Size(min = 5, max = 100)
    private String rawTrend;
    @NotEmpty
    private Set<UUID> styleIds;
    private boolean filter;
}
