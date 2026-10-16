package com.ai.generation.design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DesignRepository extends JpaRepository<Design, UUID> {

    <T> Optional<T> findById(UUID id, Class<T> responseClazz);

    @Query("SELECT d FROM Design d LEFT JOIN FETCH d.processedImages WHERE d.id = :id")
    Optional<Design> findByIdWithProcessedImages(@Param("id") UUID id);
}
