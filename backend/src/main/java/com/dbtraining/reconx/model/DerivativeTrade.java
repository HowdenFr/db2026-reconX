package com.dbtraining.reconx.model;

import java.time.LocalDate;
import com.dbtraining.reconx.model.TradeType.AssetClass;

public final class DerivativeTrade extends TradeTypeBase {

    public DerivativeTrade(TradeRef tradeRef, Money notional, LocalDate tradeDate) {
        super(tradeRef, notional, tradeDate);
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.DERIVATIVE;
    }
}
