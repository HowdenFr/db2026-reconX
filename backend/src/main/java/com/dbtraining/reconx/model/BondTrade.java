package com.dbtraining.reconx.model;

import java.time.LocalDate;
import com.dbtraining.reconx.model.TradeType.AssetClass;

public final class BondTrade extends TradeTypeBase {

    public BondTrade(TradeRef tradeRef, Money notional, LocalDate tradeDate) {
        super(tradeRef, notional, tradeDate);
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.BOND;
    }
}
