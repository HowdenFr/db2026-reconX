package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.repository.entity.ReconResultEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TICKET-ADV045 — Real, JPA-backed implementation of ReconResultRepository.
 * Translates between the ReconResult DTO record and the persisted
 * ReconResultEntity so ReconciliationService can stay unaware of JPA.
 */
@Repository
public class ReconResultRepositoryImpl implements ReconResultRepository {

    private final ReconResultJpaRepository jpaRepository;

    public ReconResultRepositoryImpl(ReconResultJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReconResult save(ReconResult result) {
        ReconResultEntity entity = new ReconResultEntity();
        entity.setTradeRef(result.tradeRef());
        entity.setStatus(result.status().name());
        entity.setDiscrepancyType(result.discrepancyType());
        entity.setDetails(result.details());
        jpaRepository.save(entity);
        return result;
    }

    @Override
    public List<ReconResult> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> new ReconResult(
                        e.getTradeRef(),
                        ReconResult.Status.valueOf(e.getStatus()),
                        e.getDiscrepancyType(),
                        e.getDetails()))
                .toList();
    }
}
