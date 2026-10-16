package com.ai.generation.generationtask.repository;

import com.ai.generation.generationtask.entity.GenerationSubtask;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface GenerationSubtaskRepository extends JpaRepository<GenerationSubtask, UUID> {

    long countByGenerationTaskIdAndFinishedAtIsNotNull(UUID generationTaskId);

    long countByGenerationTaskId(UUID generationTaskId);

    @Modifying
    @Query("""
                update GenerationSubtask s
                set s.status = :newStatus
                where s.id = :id and s.status = :expectedStatus
            """)
    int transitionStatus(UUID id, SubtaskStatus expectedStatus, SubtaskStatus newStatus);
}
