package com.dbtraining.reconx.model;


/**

 * WHAT:

 * Defines the common contract implemented by every supported trade type.

 *

 * HOW:

 * Implemented as a sealed interface so only the approved trade implementations

 * (EquityTrade, FXTrade, BondTrade, and DerivativeTrade) may participate in the

 * platform's trade hierarchy.

 *

 * WHY:

 * Provides a single, type-safe abstraction for the reconciliation engine while

 * allowing the compiler to enforce exhaustive handling of all supported trade

 * types.

 */

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
