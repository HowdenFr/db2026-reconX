package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DerivativeTradeTest {

    @Test
    void builder_buildsExpiredOptionWhenExpiryAfterTradeDate() {
        DerivativeTrade trade = DerivativeTrade.builder()
                .tradeRef(TradeRef.of("EQU-20240101-0001"))
                .underlying("AAPL")
                .strike(new BigDecimal("100"))
                .quantity(new BigDecimal("2"))
                .expiry(LocalDate.of(2024, 1, 2))
                .optionType(DerivativeTrade.OptionType.CALL)
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2024, 1, 1))
                .counterpartyId(42L)
                .build();

        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.DERIVATIVE);
        assertThat(trade.notional()).isEqualTo(new Money(new BigDecimal("200"), Currency.getInstance("USD")));
        assertThat(trade.expiry()).isEqualTo(LocalDate.of(2024, 1, 2));
    }

    @Test
    void builder_expiryBeforeTradeDate_throws() {
        assertThatThrownBy(() -> DerivativeTrade.builder()
                .tradeRef(TradeRef.of("EQU-20240102-0002"))
                .underlying("AAPL")
                .strike(new BigDecimal("100"))
                .quantity(new BigDecimal("2"))
                .expiry(LocalDate.of(2024, 1, 1))
                .optionType(DerivativeTrade.OptionType.PUT)
                .currency("USD")
                .side(Side.SELL)
                .tradeDate(LocalDate.of(2024, 1, 2))
                .counterpartyId(42L)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expiry cannot be before tradeDate");
    }
}
