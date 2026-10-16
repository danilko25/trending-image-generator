package com.ai.generation.niche.repository;

import com.ai.generation.niche.Niche;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NicheRepository extends JpaRepository<Niche, UUID>, JpaSpecificationExecutor<Niche> {

    <T> Optional<T> findById(UUID id, Class<T> responseClazz);

    @NotNull
    Page<Niche> findAll(@NotNull Pageable pageable);

    @NotNull Page<Niche> findAll(Specification<Niche> spec, @NotNull Pageable pageable);

    Optional<Niche> findByKeywordIgnoreCase(String keyword);

    boolean existsByKeywordAndLastUsedAtIsNotNullAndLastUsedAtAfter(String keyword, Instant lastUsedAt);

}
