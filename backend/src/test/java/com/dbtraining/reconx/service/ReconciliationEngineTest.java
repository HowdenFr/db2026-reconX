package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.service.ReconSummary;
import com.dbtraining.reconx.service.ReconSummaryCollector;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV040 / ADV041 / ADV042 — TDD: write the test FIRST, then the impl.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    @DisplayName("Exact matching trades return a matched reconciliation result")
    void testReconcile_exactMatch_returnsMatched() {
        // Given
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.EXACT);

        // Then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance -> MATCHED")
    @ValueSource(strings = { "0.10", "0.50", "0.99" })
    void testReconcile_priceTolerance_withinThreshold(String diff) {
        BigDecimal basePrice = new BigDecimal("100.00");
        BigDecimal externalPrice = basePrice.add(new BigDecimal(diff));
        EquityTrade internal = equity("EQU-20260603-0002", basePrice.toPlainString(), "1000");
        EquityTrade external = equity(
                "EQU-20260603-0002",
                externalPrice.toPlainString(),
                "1000");

        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT);

        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("Internal trade with no external counterpart returns a break with reason MISSING_EXTERNAL")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // Given
        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(),
                ReconciliationRule.EXACT);

        // Then
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    @DisplayName("Empty internal list returns an empty reconciliation result")
    void testReconcile_emptyInternal_returnsEmpty() {
        // Given

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(),
                List.of(),
                ReconciliationRule.EXACT);

        // Then
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("All mismatched trades produce a broken reconciliation summary")
    void testReconcile_allMismatched_returnsBrokenSummary() {
        // Given
        EquityTrade internal1 = equity("EQU-20260603-0101", "100.00", "1000");
        EquityTrade internal2 = equity("EQU-20260603-0102", "200.00", "1000");
        EquityTrade internal3 = equity("EQU-20260603-0103", "300.00", "1000");

        EquityTrade external1 = equity("EQU-20260603-0201", "100.00", "1000");
        EquityTrade external2 = equity("EQU-20260603-0202", "200.00", "1000");
        EquityTrade external3 = equity("EQU-20260603-0203", "300.00", "1000");

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(internal1, internal2, internal3),
                List.of(external1, external2, external3),
                ReconciliationRule.EXACT);

        ReconSummary summary = out.stream()
                .collect(new ReconSummaryCollector());

        // Then
        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(3);
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
