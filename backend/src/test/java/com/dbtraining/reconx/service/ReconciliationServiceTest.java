package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * TICKET-ADV043 — Mockito ArgumentCaptor: assert the CONTENT of what was
 * saved, not just that save() was called.
 */
class ReconciliationServiceTest {

    @Test
    void testRunRecon_savesCapturedResultWithCorrectFields() {
        // given
        ReconResultRepository repo = mock(ReconResultRepository.class);
        ReconciliationService service = new ReconciliationService(new ReconciliationEngine(), repo);

        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // when
        service.runRecon(List.of(internal), List.of(external), ReconciliationRule.EXACT);

        // then
        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().tradeRef()).isEqualTo("EQU-20260603-0001");
        assertThat(captor.getValue().status()).isEqualTo(ReconResult.Status.MATCHED);
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
