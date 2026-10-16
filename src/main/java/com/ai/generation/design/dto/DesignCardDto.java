package com.ai.generation.design.dto;

import com.ai.generation.design.Design;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Slf4j
public class DesignCardDto {
    private UUID id;
    private String imageUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public DesignCardDto(Design design) {
        this.id = design.getId();
        this.imageUrl = design.getImagePath();
        this.createdAt = LocalDateTime.ofInstant(design.getCreatedAt(), ZoneId.systemDefault());
    }

    @Getter
    public enum SortField {
        ID("id"), CREATE_DATE("createdAt");

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
        return SortField.CREATE_DATE;
    }
}
