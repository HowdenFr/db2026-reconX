package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.exception.TradeNotFoundException;
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
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.URI;

/**
 * TICKET-ADV068 — POST /api/v1/recon/run — returns 202 + jobId
 * TICKET-ADV069 — GET /api/v1/recon/jobs/{jobId}/results
 * TICKET-ADV070 — PUT /api/v1/recon/results/{id}/resolve
 */
@RestController
@RequestMapping("/v1/recon")
@Tag(name = "recon", description = "Reconciliation operations")
@SecurityRequirement(name = "bearerAuth")
public class ReconController {

    private final ReconBreakRepository breaks;
    private final ReconciliationService reconciliationService;

    public ReconController(ReconBreakRepository breaks, ReconciliationService reconciliationService) {
        this.breaks = breaks;
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a reconciliation job (async)")
    public ResponseEntity<Map<String, String>> runRecon(
            @Valid @RequestBody ReconRunRequest req) {

        // Generate a unique identifier for this reconciliation job
        UUID jobId = UUID.randomUUID();

        // Response body sent back to the client
        Map<String, String> response = Map.of(
                "jobId", jobId.toString(),
                "status", "QUEUED");

        // Where the client should check later for results
        URI location = URI.create("/v1/recon/jobs/" + jobId + "/results");

        return ResponseEntity
                .accepted()
                .location(location)
                .body(response);
    }

    @GetMapping("/jobs/{jobId}/results")
    @Operation(summary = "Get results for a recon job")
    public List<ReconBreak> results(@PathVariable String jobId) {

        return breaks.findAll();

    }

    @PutMapping("/results/{id}/resolve")
    @Operation(summary = "Mark a recon break as RESOLVED with a note")
    public ResponseEntity<ReconBreak> resolve(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ReconBreak reconBreak = breaks.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Recon break not found: " + id));
        reconBreak.resolve(body.get("note"));
        return ResponseEntity.ok(breaks.save(reconBreak));
    }
}
