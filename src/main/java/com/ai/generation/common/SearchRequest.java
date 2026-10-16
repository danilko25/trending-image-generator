package com.ai.generation.common;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class SearchRequest {
    private Integer page = 0;
    private Integer pageSize = 10;
    private String sortBy;
    protected Sort.Direction direction = Sort.Direction.DESC;

    public Pageable getPageable(String sortingField) {
        return PageRequest.of(
                page,
                pageSize,
                Sort.by(direction, sortingField)
        );
    }
}
