package com.dbtraining.reconx.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeRequest(
        @NotBlank String tradeRef,
        @NotNull Long counterpartyId,
        @NotNull Long instrumentId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @PastOrPresent LocalDate tradeDate
) {}
