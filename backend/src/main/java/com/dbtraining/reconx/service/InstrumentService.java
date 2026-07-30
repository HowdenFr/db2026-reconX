package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * TICKET-ADV081 — @Cacheable on findBySymbol (cache name "instruments").
 * TICKET-ADV082 — TTL configured in application.yml (caffeine spec).
 *
 * Symbol lookup is hot — most requests touch the cache, not the DB.
 */
@Service
public class InstrumentService {

    // Service Talks to The Repository/Not the DataBase Directly
    private final InstrumentRepository repo;

    public InstrumentService(InstrumentRepository repo) {
        this.repo = repo;
    }

    // Important Part | Talks to the Cache Before Checking the Database

    @Cacheable("instruments")
    public Instrument findBySymbol(String symbol) {

        return repo.findBySymbol(symbol)
                .orElseThrow(() -> new InvalidTradeException("Unknown instrument symbol: " + symbol));

    }
}