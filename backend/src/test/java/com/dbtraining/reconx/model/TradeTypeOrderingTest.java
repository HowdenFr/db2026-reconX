package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class TradeTypeOrderingTest {

    @Test
    void heterogeneousTradesSortByNewestDateThenTradeRef() {
        TradeType equity = equity("EQU-20260601-0002", LocalDate.of(2026, 6, 1));
        TradeType fx = fx("FXT-20260603-0001", LocalDate.of(2026, 6, 3));
        TradeType bond = bond("BND-20260602-0001", LocalDate.of(2026, 6, 2));
        TradeType derivative = derivative("DRV-20260601-0001", LocalDate.of(2026, 6, 1));

        TreeSet<TradeType> trades = new TreeSet<>(List.of(equity, fx, bond, derivative));

        assertThat(trades)
                .extracting(trade -> trade.tradeRef().value())
                .containsExactly(
                        "FXT-20260603-0001",
                        "BND-20260602-0001",
                        "DRV-20260601-0001",
                        "EQU-20260601-0002");
    }

    @Test
    void comparisonIsZeroForMatchingTradeRefsOnTheSameDate() {
        TradeType first = equity("EQU-20260601-0001", LocalDate.of(2026, 6, 1));
        TradeType second = fx("EQU-20260601-0001", LocalDate.of(2026, 6, 1));

        assertThat(first.compareTo(second)).isZero();
    }

    private EquityTrade equity(String ref, LocalDate tradeDate) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(BigDecimal.TEN)
                .price(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(1L)
                .build();
    }

    private FXTrade fx(String ref, LocalDate tradeDate) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .ccy1("EUR")
                .ccy2("USD")
                .notionalCcy1(new BigDecimal("1000"))
                .fxRate(new BigDecimal("1.10"))
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(2L)
                .build();
    }

    private BondTrade bond(String ref, LocalDate tradeDate) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .isin("DE0001234567")
                .faceValue(new BigDecimal("1000"))
                .couponRate(new BigDecimal("0.03"))
                .maturityDate(tradeDate.plusYears(5))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(3L)
                .build();
    }

    private DerivativeTrade derivative(String ref, LocalDate tradeDate) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .underlying("SAP.DE")
                .strike(new BigDecimal("100"))
                .quantity(BigDecimal.ONE)
                .expiry(tradeDate.plusMonths(6))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(tradeDate)
                .counterpartyId(4L)
                .build();
    }
}
