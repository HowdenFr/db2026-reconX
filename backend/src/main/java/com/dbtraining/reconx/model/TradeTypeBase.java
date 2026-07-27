package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Internal abstract base for {@link TradeType} implementations.
 *
 * <p>Provides the common immutable state shared by concrete trade types,
 * including the trade reference, notional, and trade date.</p>
 */
abstract class TradeTypeBase {

    private final TradeRef tradeRef;
    private final Money notional;
    private final LocalDate tradeDate;

    /**
     * Creates the shared immutable trade state.
     *
     * @param tradeRef the unique trade reference; must not be {@code null}
     * @param notional the monetary notional value; must not be {@code null}
     * @param tradeDate the trade execution date; must not be {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    TradeTypeBase(TradeRef tradeRef, Money notional, LocalDate tradeDate) {
        this.tradeRef = Objects.requireNonNull(tradeRef, "tradeRef");
        this.notional = Objects.requireNonNull(notional, "notional");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate");
    }

    public final TradeRef tradeRef() {
        return tradeRef;
    }

    public final Money notional() {
        return notional;
    }

    public final LocalDate tradeDate() {
        return tradeDate;
    }
}