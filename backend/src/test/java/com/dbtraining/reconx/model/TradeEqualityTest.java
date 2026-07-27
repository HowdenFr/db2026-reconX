package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEqualityTest {

    private static final String REF = "EQU-20260601-0001";
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 1);

    @Test
    void equityTradesWithSameRefAreEqualDespiteDifferentFields() {
        EquityTrade first = equity("100", "10");
        EquityTrade second = equity("125", "20");

        assertSameBusinessKey(first, second);
    }

    @Test
    void fxTradesWithSameRefAreEqualDespiteDifferentFields() {
        FXTrade first = fx("1000", "1.10");
        FXTrade second = fx("2000", "1.20");

        assertSameBusinessKey(first, second);
    }

    @Test
    void bondTradesWithSameRefAreEqualDespiteDifferentFields() {
        BondTrade first = bond("1000", "0.03");
        BondTrade second = bond("2000", "0.05");

        assertSameBusinessKey(first, second);
    }

    @Test
    void derivativeTradesWithSameRefAreEqualDespiteDifferentFields() {
        DerivativeTrade first = derivative("100", "1");
        DerivativeTrade second = derivative("125", "2");

        assertSameBusinessKey(first, second);
    }

    @Test
    void tradesOfDifferentConcreteTypesAreNotEqual() {
        TradeType equity = equity("100", "10");
        TradeType fx = fx("1000", "1.10");

        assertThat(equity).isNotEqualTo(fx);
        assertThat(fx).isNotEqualTo(equity);
    }

    private void assertSameBusinessKey(TradeType first, TradeType second) {
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(new HashSet<>(List.of(first, second))).hasSize(1);
        assertThat(first.compareTo(second)).isZero();
    }

    private EquityTrade equity(String price, String quantity) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(REF))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal(quantity))
                .price(new BigDecimal(price))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(TRADE_DATE)
                .counterpartyId(1L)
                .build();
    }

    private FXTrade fx(String notional, String rate) {
        return FXTrade.builder()
                .tradeRef(TradeRef.of(REF))
                .ccy1("EUR")
                .ccy2("USD")
                .notionalCcy1(new BigDecimal(notional))
                .fxRate(new BigDecimal(rate))
                .side(Side.BUY)
                .tradeDate(TRADE_DATE)
                .counterpartyId(2L)
                .build();
    }

    private BondTrade bond(String faceValue, String couponRate) {
        return BondTrade.builder()
                .tradeRef(TradeRef.of(REF))
                .isin("DE0001234567")
                .faceValue(new BigDecimal(faceValue))
                .couponRate(new BigDecimal(couponRate))
                .maturityDate(TRADE_DATE.plusYears(5))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(TRADE_DATE)
                .counterpartyId(3L)
                .build();
    }

    private DerivativeTrade derivative(String strike, String quantity) {
        return DerivativeTrade.builder()
                .tradeRef(TradeRef.of(REF))
                .underlying("SAP.DE")
                .strike(new BigDecimal(strike))
                .quantity(new BigDecimal(quantity))
                .expiry(TRADE_DATE.plusMonths(6))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(TRADE_DATE)
                .counterpartyId(4L)
                .build();
    }
}
