package com.ai.generation.imagestyle.dto;

import com.ai.generation.common.SearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ImageStyleSearchRequest extends SearchRequest {
    private String name;
    private Boolean active;
}
