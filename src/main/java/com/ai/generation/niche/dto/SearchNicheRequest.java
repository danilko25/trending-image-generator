package com.ai.generation.niche.dto;

import com.ai.generation.common.SearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class SearchNicheRequest extends SearchRequest {
    private String keyword;
    private Instant createdAfter;
    private Instant createdBefore;
}
