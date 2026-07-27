package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Objects;

abstract class TradeTypeBase {

    private final TradeRef tradeRef;
    private final Money notional;
    private final LocalDate tradeDate;

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
