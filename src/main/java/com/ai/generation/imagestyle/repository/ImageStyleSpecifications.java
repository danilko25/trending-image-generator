package com.ai.generation.imagestyle.repository;

import com.ai.generation.imagestyle.ImageStyle;
import org.springframework.data.jpa.domain.Specification;

public class ImageStyleSpecifications {
    public static Specification<ImageStyle> byName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<ImageStyle> byActive(Boolean active) {
        return (root, query, cb) ->
                active == null ? null : cb.equal(root.get("active"), active);
    }
}
