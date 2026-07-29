package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Trade;

import java.util.NoSuchElementException;

public class TradeLookupService {

    private final TradeRepository tradeRepo;

    public TradeLookupService(TradeRepository tradeRepo) {
        this.tradeRepo = tradeRepo;
    }

    public Counterparty findCounterpartyByTradeRef(String tradeRef) {
        return tradeRepo.findByTradeRef(tradeRef)
                .map(Trade::getCounterparty)
                .orElseThrow(() -> new NoSuchElementException("No CounterParty for TradeRef=" + tradeRef));
    }

    public Counterparty counterpartyForTradeRef(String tradeRef) {
        return findCounterpartyByTradeRef(tradeRef);
    }
}
