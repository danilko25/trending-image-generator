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
public class ImageStyleDropDownItemDto {
    private UUID id;
    private String name;

    public ImageStyleDropDownItemDto(ImageStyle imageStyle) {
        this.id = imageStyle.getId();
        this.name = imageStyle.getName();
    }

    @Getter
    public enum SortField {
        ID("id"), NAME("name");

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
}
