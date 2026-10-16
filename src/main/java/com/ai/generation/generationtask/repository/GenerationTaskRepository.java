package com.ai.generation.generationtask.repository;

import com.ai.generation.generationtask.entity.GenerationStatus;
import com.ai.generation.generationtask.entity.GenerationTask;
import com.ai.generation.generationtask.entity.SubtaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTask, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select t
                from GenerationTask t
                where t.id = :id
            """)
    Optional<GenerationTask> findByIdForUpdate(UUID id);

    @Modifying
    @Query("""
                update GenerationTask s
                set s.status = :newStatus
                where s.id = :id and s.status = :expectedStatus
            """)
    int transitionStatus(UUID id, GenerationStatus expectedStatus, GenerationStatus newStatus);
}
