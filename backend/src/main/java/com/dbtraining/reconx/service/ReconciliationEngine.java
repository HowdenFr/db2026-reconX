package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV033 — ReconciliationEngine using Streams (parallel matching)
 * TICKET-ADV037 — CompletableFuture: parallel recon by counterparty
 * TICKET-ADV047 — Edge cases: empty/single/all-mismatched inputs handled
 * TICKET-ADV084 — @Timed exports reconciliation_duration_seconds histogram
 *
 * WHAT:    Compares internal trades against external (counterparty) trades and
 *          returns a ReconResult per internal trade (MATCHED or BREAK).
 * HOW:     Index externals by tradeRef, then stream internals and look each
 *          up. CompletableFuture variant batches by counterparty for
 *          throughput on large books.
 * WHY:     This is the spine of the product. Everything else (REST API,
 *          Kafka consumers, dashboard) ultimately calls into here.
 * OBSERVE: Histogram appears at /actuator/prometheus under
 *          reconciliation_duration_seconds.
 * ============================================================================
 */
@Service
public class ReconciliationEngine {

    /**
     * TICKET-ADV037 — bounded, named pool owned by this engine so per-counterparty
     * reconciliation never borrows the JVM-wide common ForkJoinPool. Named threads
     * (recon-worker-N) show up distinctly under jstack.
     */
    private final ExecutorService reconExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new CustomizableThreadFactory("recon-worker-"));

    @Timed(value = "reconciliation.duration", description = "Wall time of reconcile()",
           percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public List<ReconResult> reconcile(List<TradeType> internal,
                                       List<TradeType> external,
                                       ReconciliationRule rule) {
        if (internal == null || internal.isEmpty()) {
            return List.of();
        }

        Map<String, TradeType> externalByRef =
                (external == null ? List.<TradeType>of() : external)
                        .stream()
                        .collect(Collectors.toMap(
                                trade -> trade.tradeRef().value(),
                                Function.identity(),
                                (first, duplicate) -> first));

        return internal.parallelStream()
                .map(trade -> matchOne(
                        trade,
                        externalByRef.get(trade.tradeRef().value()),
                        rule))
                .toList();
    }

    /**
     * TICKET-ADV037 — split by counterparty, reconcile each batch concurrently,
     * combine into a single result list. Caller passes one external feed per
     * counterparty (typical real-world shape).
     */
    public CompletableFuture<List<ReconResult>> reconcileByCounterparty(
            Map<Long, List<TradeType>> internalByCp,
            Map<Long, List<TradeType>> externalByCp,
            ReconciliationRule rule) {
        List<CompletableFuture<List<ReconResult>>> futures = internalByCp.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(
                        () -> reconcile(
                                entry.getValue(),
                                externalByCp.getOrDefault(entry.getKey(), List.of()),
                                rule),
                        reconExecutor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream().flatMap(f -> f.join().stream()).toList());
    }

    /** TICKET-ADV037 — releases the executor this engine owns. */
    public void shutdown() {
        reconExecutor.shutdown();
    }

    private ReconResult matchOne(TradeType internal, TradeType external, ReconciliationRule rule) {
        String ref = internal.tradeRef().value();
        if (external == null) {
            return ReconResult.breakResult(
                    ref,
                    "MISSING_EXTERNAL",
                    "No external trade found for " + ref);
        }

        BigDecimal[] internalPair = priceQty(internal);
        BigDecimal[] externalPair = priceQty(external);
        if (rule.matches(
                internalPair[0],
                internalPair[1],
                externalPair[0],
                externalPair[1])) {
            return ReconResult.matched(ref);
        }

        return ReconResult.breakResult(
                ref,
                "VALUE_MISMATCH",
                "internal=%s/%s external=%s/%s".formatted(
                        internalPair[0],
                        internalPair[1],
                        externalPair[0],
                        externalPair[1]));
    }

    /** TICKET-ADV018 — exhaustive switch over the sealed hierarchy. */
    private BigDecimal[] priceQty(TradeType t) {
        return switch (t) {
            case com.dbtraining.reconx.model.EquityTrade equity ->
                    new BigDecimal[]{equity.price(), equity.quantity()};
            case com.dbtraining.reconx.model.FXTrade fx ->
                    new BigDecimal[]{fx.fxRate(), fx.notionalCcy1()};
            case com.dbtraining.reconx.model.BondTrade bond ->
                    new BigDecimal[]{bond.couponRate(), bond.faceValue()};
            case com.dbtraining.reconx.model.DerivativeTrade derivative ->
                    new BigDecimal[]{derivative.strike(), derivative.quantity()};
        };
    }
}
