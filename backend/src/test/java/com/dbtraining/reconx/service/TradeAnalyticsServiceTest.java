package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TradeAnalyticsServiceTest {

    private final TradeAnalyticsService analyticsService = new TradeAnalyticsService();

    @Test
    void notionalByCounterparty_groupsTradesAndSummarizesNotionals() {
        EquityTrade trade1 = equity("EQU-20260101-0001", "100", "2", 1L);
        EquityTrade trade2 = equity("EQU-20260101-0002", "150", "3", 1L);
        EquityTrade trade3 = equity("EQU-20260101-0003", "75", "4", 2L);

        Map<String, TradeAnalyticsService.NotionalSummary> summary = analyticsService.notionalByCounterparty(
                List.of(trade1, trade2, trade3));

        assertThat(summary).hasSize(2);

        TradeAnalyticsService.NotionalSummary counterparty1 = summary.get("1");
        assertThat(counterparty1).isNotNull();
        assertThat(counterparty1.count()).isEqualTo(2);
        assertThat(counterparty1.total()).isEqualByComparingTo(new BigDecimal("650"));
        assertThat(counterparty1.min()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(counterparty1.max()).isEqualByComparingTo(new BigDecimal("450"));
        assertThat(counterparty1.average()).isEqualByComparingTo(new BigDecimal("325"));

        TradeAnalyticsService.NotionalSummary counterparty2 = summary.get("2");
        assertThat(counterparty2).isNotNull();
        assertThat(counterparty2.count()).isEqualTo(1);
        assertThat(counterparty2.total()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(counterparty2.min()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(counterparty2.max()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(counterparty2.average()).isEqualByComparingTo(new BigDecimal("300"));
    }

    private EquityTrade equity(String ref, String price, String quantity, long counterpartyId) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("AAPL")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(quantity))
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 1, 1))
                .counterpartyId(counterpartyId)
                .build();
    }
}
