package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        @Test
        void vwapCollector_computesExpected_and_parallelMatchesSerial() {
        EquityTrade a1 = EquityTrade.builder()
            .tradeRef(TradeRef.of("AAA-20260101-0001"))
            .instrumentSymbol("AAA")
            .price(new BigDecimal("10"))
            .quantity(new BigDecimal("2"))
            .currency("USD")
            .side(Side.BUY)
            .tradeDate(LocalDate.of(2026,1,1))
            .counterpartyId(1L)
            .build();

        EquityTrade a2 = EquityTrade.builder()
            .tradeRef(TradeRef.of("AAA-20260101-0002"))
            .instrumentSymbol("AAA")
            .price(new BigDecimal("12"))
            .quantity(new BigDecimal("3"))
            .currency("USD")
            .side(Side.BUY)
            .tradeDate(LocalDate.of(2026,1,1))
            .counterpartyId(1L)
            .build();

        EquityTrade b1 = EquityTrade.builder()
            .tradeRef(TradeRef.of("BBB-20260101-0001"))
            .instrumentSymbol("BBB")
            .price(new BigDecimal("5"))
            .quantity(new BigDecimal("1"))
            .currency("USD")
            .side(Side.BUY)
            .tradeDate(LocalDate.of(2026,1,1))
            .counterpartyId(2L)
            .build();

        EquityTrade b2 = EquityTrade.builder()
            .tradeRef(TradeRef.of("BBB-20260101-0002"))
            .instrumentSymbol("BBB")
            .price(new BigDecimal("7"))
            .quantity(new BigDecimal("1"))
            .currency("USD")
            .side(Side.BUY)
            .tradeDate(LocalDate.of(2026,1,1))
            .counterpartyId(2L)
            .build();

        List<EquityTrade> trades = List.of(a1, a2, b1, b2);

        // collector directly on stream (serial and parallel)
        BigDecimal serialAaa = trades.stream()
            .filter(t -> t.instrumentSymbol().equals("AAA"))
            .collect(TradeAnalyticsService.vwapCollector());
        BigDecimal parallelAaa = trades.parallelStream()
            .filter(t -> t.instrumentSymbol().equals("AAA"))
            .collect(TradeAnalyticsService.vwapCollector());

        assertThat(serialAaa).isEqualByComparingTo(parallelAaa);
        assertThat(serialAaa).isEqualByComparingTo(new BigDecimal("11.200000"));

        // vwapByInstrument mapping
        Map<String, BigDecimal> map = analyticsService.vwapByInstrument(trades);
        assertThat(map.get("AAA")).isEqualByComparingTo(new BigDecimal("11.200000"));
        assertThat(map.get("BBB")).isEqualByComparingTo(new BigDecimal("6.000000"));
        }

        @Test
        void vwapCollector_emptyStream_returnsZero() {
        BigDecimal result = List.<EquityTrade>of().stream().collect(TradeAnalyticsService.vwapCollector());
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

            @Test
            void pnlByInstrument_mixedBuySell_matchesHandCalculatedAndParallelSafe() {
            EquityTrade t1 = EquityTrade.builder()
                .tradeRef(TradeRef.of("AAA-20260101-1001"))
                .instrumentSymbol("AAA")
                .price(new BigDecimal("10"))
                .quantity(new BigDecimal("2"))
                .side(Side.SELL)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(1L)
                .build(); // +20

            EquityTrade t2 = EquityTrade.builder()
                .tradeRef(TradeRef.of("AAA-20260101-1002"))
                .instrumentSymbol("AAA")
                .price(new BigDecimal("8"))
                .quantity(new BigDecimal("1"))
                .side(Side.BUY)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(1L)
                .build(); // -8 => total 12

            EquityTrade t3 = EquityTrade.builder()
                .tradeRef(TradeRef.of("BBB-20260101-1001"))
                .instrumentSymbol("BBB")
                .price(new BigDecimal("5"))
                .quantity(new BigDecimal("3"))
                .side(Side.SELL)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(2L)
                .build(); // +15

            EquityTrade t4 = EquityTrade.builder()
                .tradeRef(TradeRef.of("BBB-20260101-1002"))
                .instrumentSymbol("BBB")
                .price(new BigDecimal("6"))
                .quantity(new BigDecimal("1"))
                .side(Side.BUY)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(2L)
                .build(); // -6 => total 9

            EquityTrade t5 = EquityTrade.builder()
                .tradeRef(TradeRef.of("CCC-20260101-1001"))
                .instrumentSymbol("CCC")
                .price(new BigDecimal("7"))
                .quantity(new BigDecimal("2"))
                .side(Side.BUY)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(3L)
                .build(); // -14

            EquityTrade t6 = EquityTrade.builder()
                .tradeRef(TradeRef.of("CCC-20260101-1002"))
                .instrumentSymbol("CCC")
                .price(new BigDecimal("10"))
                .quantity(new BigDecimal("1"))
                .side(Side.SELL)
                .currency("USD")
                .tradeDate(LocalDate.of(2026,1,1))
                .counterpartyId(3L)
                .build(); // +10 => total -4

            List<EquityTrade> trades = List.of(t1, t2, t3, t4, t5, t6);

            Map<String, BigDecimal> serial = analyticsService.pnlByInstrument(trades);

            Map<String, BigDecimal> parallel = trades.parallelStream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                Collectors.mapping(t -> {
                        BigDecimal abs = t.price().multiply(t.quantity());
                        return t.side() == Side.SELL ? abs : abs.negate();
                    },
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));

            assertThat(serial).isEqualTo(parallel);

            assertThat(serial.get("AAA")).isEqualByComparingTo(new BigDecimal("12"));
            assertThat(serial.get("BBB")).isEqualByComparingTo(new BigDecimal("9"));
            assertThat(serial.get("CCC")).isEqualByComparingTo(new BigDecimal("-4"));
            }
}
