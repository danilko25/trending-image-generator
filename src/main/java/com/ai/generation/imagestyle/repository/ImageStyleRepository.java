package com.ai.generation.imagestyle.repository;

import com.ai.generation.imagestyle.ImageStyle;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageStyleRepository extends JpaRepository<ImageStyle, UUID>, JpaSpecificationExecutor<ImageStyle> {

    <T> Optional<T> findById(UUID id, Class<T> responseClazz);

    @NonNull Page<ImageStyle> findAll(Specification<ImageStyle> specification, @NonNull Pageable pageable);

    <T> List<T> findByActive(boolean active, Class<T> responseClazz);

}
