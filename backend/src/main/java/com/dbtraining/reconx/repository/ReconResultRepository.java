package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;

import java.util.List;

/**
 * TICKET-ADV043 — Persistence seam for reconciliation outcomes.
 * TICKET-ADV045 — findAll() added so the integration test can assert on
 *                 what actually landed in Postgres.
 *
 * WHAT:    Saves/reads ReconResult rows produced by ReconciliationEngine.
 * WHY:     Kept as a plain interface (not JpaRepository directly) because
 *          ReconResult is a DTO record, not a JPA entity — this is the
 *          mockable seam ReconciliationServiceTest verifies via Mockito
 *          ArgumentCaptor. The real implementation (ReconResultRepositoryImpl)
 *          delegates to a JPA-backed ReconResultEntity underneath.
 */
public interface ReconResultRepository {
    ReconResult save(ReconResult result);
    List<ReconResult> findAll();
}
