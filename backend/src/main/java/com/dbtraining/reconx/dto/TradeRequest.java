package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeRequest(
        @NotNull
        @Pattern(
                regexp = "^[A-Z]{3}-\\d{8}-\\d{4}$",
                message = "tradeRef must match AAA-YYYYMMDD-NNNN")
        String tradeRef,

        @NotNull Long instrumentId,
        @NotNull Long counterpartyId,

        @NotBlank String assetClass,
        @NotBlank
        @Pattern(regexp = "^(BUY|SELL)$")
        String side,

        @NotNull @Positive BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PastOrPresent LocalDate tradeDate
) {}
