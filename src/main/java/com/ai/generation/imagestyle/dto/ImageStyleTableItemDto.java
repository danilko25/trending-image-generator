package com.ai.generation.imagestyle.dto;

import com.ai.generation.imagestyle.ImageStyle;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Slf4j
public class ImageStyleTableItemDto {
    UUID id;
    String name;
    Boolean active;

    public ImageStyleTableItemDto(ImageStyle imageStyle) {
        this.id = imageStyle.getId();
        this.name = imageStyle.getName();
        this.active = imageStyle.isActive();
    }

    @Getter
    public enum SortField {
        ID("id"), NAME("name"), ACTIVE("active");

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
        return SortField.NAME;
    }
}
