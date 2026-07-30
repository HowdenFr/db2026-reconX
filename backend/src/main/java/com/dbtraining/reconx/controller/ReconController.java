package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import com.dbtraining.reconx.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET  /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT  /api/v1/recon/results/{id}/resolve
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;
    private final ReconciliationService reconciliationService;

    public ReconController(ReconBreakRepository breaks,
                           ReconciliationService reconciliationService) {
        this.breaks = breaks;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    public ResponseEntity<Map<String, String>> runRecon(
            @Valid @RequestBody(required = false) ReconRunRequest req) {
        ReconRunRequest request = req == null
                ? new ReconRunRequest(LocalDate.now().minusDays(1), LocalDate.now(), 1L)
                : req;

        List<TradeType> internal = sampleTrades(request, BigDecimal.ZERO);
        List<TradeType> external = sampleTrades(request, new BigDecimal("0.25"));
        reconciliationService.runRecon(internal, external, ReconciliationRule.EXACT);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", UUID.randomUUID().toString(),
                "status", "QUEUED"));
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public List<ReconBreak> results(@PathVariable String jobId) {
        // TODO(TICKET-ADV069): once recon_jobs + recon_breaks tables are wired,
        //   return breaks.findByJobId(jobId). Day-0 returns an empty list so
        //   the React breaks-table renders "no breaks" gracefully.
        return Collections.emptyList();
    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreak> resolve(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        // TODO(TICKET-ADV070): load the ReconBreak, call rb.resolve(note), save,
        //   and return 200 with the updated entity. Throw TradeNotFoundException
        //   when the id is unknown.
        throw new UnsupportedOperationException("TICKET-ADV070");
    }

    private List<TradeType> sampleTrades(ReconRunRequest req, BigDecimal priceBump) {
        long counterpartyId = req.counterpartyId() == null ? 1L : req.counterpartyId();
        LocalDate tradeDate = req.to() == null ? LocalDate.now() : req.to();

        return IntStream.range(0, 48)
                .mapToObj(i -> EquityTrade.builder()
                        .tradeRef(TradeRef.of("REC-%08d-%04d".formatted(tradeDate.toEpochDay(), i)))
                        .instrumentSymbol("SAP.DE")
                        .quantity(new BigDecimal("100"))
                        .price(new BigDecimal("245.50").add(priceBump.multiply(BigDecimal.valueOf(i % 3))))
                        .currency("EUR")
                        .side(i % 2 == 0 ? Side.BUY : Side.SELL)
                        .tradeDate(tradeDate)
                        .counterpartyId(counterpartyId)
                        .build())
                .map(TradeType.class::cast)
                .toList();
    }
}
