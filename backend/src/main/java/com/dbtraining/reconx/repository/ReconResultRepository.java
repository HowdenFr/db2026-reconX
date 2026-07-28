package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;

/**
 * TICKET-ADV043 — Persistence seam for reconciliation outcomes.
 *
 * WHAT:    Saves a single ReconResult produced by ReconciliationEngine.
 * WHY:     Kept as a plain interface (not JpaRepository) because ReconResult
 *          is a DTO, not a JPA entity — this is the mockable seam
 *          ReconciliationServiceTest verifies via ArgumentCaptor. Wiring a
 *          real persistence-backed implementation is Day 4+ scope.
 */
public interface ReconResultRepository {
    ReconResult save(ReconResult result);
}
