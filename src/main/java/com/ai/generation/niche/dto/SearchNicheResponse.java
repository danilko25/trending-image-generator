package com.ai.generation.niche.dto;

import com.ai.generation.niche.Niche;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Slf4j
public class SearchNicheResponse {
    private UUID id;
    private String keyword;
    private Instant createdAt;
    private Instant lastUsedAt;

    public SearchNicheResponse(Niche niche) {
        this.id = niche.getId();
        this.keyword = niche.getKeyword();
        this.createdAt = niche.getCreatedAt();
        this.lastUsedAt = niche.getLastUsedAt();
    }

    @Getter
    public enum SortField {
        ID("id"), KEYWORD("keyword"), CREATED_AT("createdAt"), LAST_USED_AT("lastUsedAt");

        private final String name;

        SortField(String name) {
            this.name = name;
        }

        private static final Map<String, SortField> BY_NAME = Arrays.stream(values())
                .collect(Collectors.toMap(SortField::getName, Function.identity()));

        public static SortField fromName(String name) {
            return BY_NAME.get(name);
        }
    }

    public static SortField getSortingParam(String sortBy) {
        if (sortBy != null) {
            var sortField = SortField.fromName(sortBy);
            if (sortField != null) {
                return sortField;
            }
            log.error("Invalid sort by: {}", sortBy);
        }
        return SortField.CREATED_AT;
    }
}
