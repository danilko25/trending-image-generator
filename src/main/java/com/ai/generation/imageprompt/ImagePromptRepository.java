package com.ai.generation.imageprompt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagePromptRepository extends JpaRepository<ImagePrompt, UUID> {
    <T> List<T> findAllById(UUID id, Class<T> responseClazz);

    @Query("SELECT ip FROM ImagePrompt ip INNER JOIN FETCH ip.style WHERE ip.id = :id")
    Optional<ImagePrompt> findWithStyleById(@Param("id") UUID id);
}
