package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.ReconResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TICKET-ADV045 — Spring Data repository backing ReconResultRepositoryImpl.
 */
public interface ReconResultJpaRepository extends JpaRepository<ReconResultEntity, Long> {
}
