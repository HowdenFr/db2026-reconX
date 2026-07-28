package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TICKET-ADV043 — Wraps ReconciliationEngine and persists each ReconResult.
 *
 * WHY:    Separating persistence from the pure-stream engine keeps
 *         ReconciliationEngine unit-testable without a repository, while
 *         still giving this layer a seam (ReconResultRepository) that
 *         ReconciliationServiceTest verifies via Mockito ArgumentCaptor.
 */
@Service
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repository;

    public ReconciliationService(ReconciliationEngine engine, ReconResultRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    public List<ReconResult> runRecon(List<TradeType> internal, List<TradeType> external, ReconciliationRule rule) {
        List<ReconResult> results = engine.reconcile(internal, external, rule);
        results.forEach(repository::save);
        return results;
    }
}
