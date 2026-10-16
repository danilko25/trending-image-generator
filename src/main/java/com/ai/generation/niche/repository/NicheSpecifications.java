package com.ai.generation.niche.repository;

import com.ai.generation.niche.Niche;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class NicheSpecifications {
    public static Specification<Niche> byKeyword(String keyword) {
        return (root, query, cb) ->
                keyword == null ? null : cb.like(cb.lower(root.get("keyword")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Niche> createdAfter(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Niche> createdBefore(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
