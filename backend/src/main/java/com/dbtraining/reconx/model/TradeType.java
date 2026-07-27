package com.dbtraining.reconx.model;

import java.time.LocalDate;
import java.util.Comparator;

public sealed interface TradeType
        extends Comparable<TradeType>
        permits EquityTrade, FXTrade, BondTrade, DerivativeTrade {

    TradeRef tradeRef();

    Money notional();

    LocalDate tradeDate();

    AssetClass assetClass();

    Comparator<TradeType> NATURAL = Comparator
            .comparing(TradeType::tradeDate).reversed()
            .thenComparing(trade -> trade.tradeRef().value());

    @Override
    default int compareTo(TradeType other) {
        return NATURAL.compare(this, other);
    }

    enum AssetClass { EQUITY, FX, BOND, DERIVATIVE }
}
