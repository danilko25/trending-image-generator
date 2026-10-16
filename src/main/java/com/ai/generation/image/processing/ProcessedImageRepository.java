package com.ai.generation.image.processing;

import com.ai.generation.image.processing.entity.ProcessedImage;
import com.ai.generation.image.processing.entity.ProcessingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedImageRepository extends JpaRepository<ProcessedImage, UUID> {

    <T> List<T> findAllByDesignId(UUID designId, Class<T> responseClass);

    @Query("SELECT pi FROM ProcessedImage pi INNER JOIN FETCH pi.design WHERE pi.id = :id ")
    Optional<ProcessedImage> findWithDesignById(@Param("id") UUID id);

    Optional<ProcessedImage> findByDesignIdAndType(UUID designId, ProcessingType type);
}
