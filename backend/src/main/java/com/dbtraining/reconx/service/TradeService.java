package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.dbtraining.reconx.repository.TradeSpecifications.forCounterparty;
import static com.dbtraining.reconx.repository.TradeSpecifications.hasStatus;
import static com.dbtraining.reconx.repository.TradeSpecifications.tradeDateBetween;

/**
 * ============================================================================
 * TICKET-ADV064 - TradeService.create (POST endpoint backing)
 * TICKET-ADV065 - update
 * TICKET-ADV066 - updateStatus (PATCH)
 * TICKET-ADV067 - softDelete
 * TICKET-ADV083 - increments trade_created_total Counter on create
 * TICKET-ADV129 - publishes TradeEvent on every state change
 * TICKET-ADV055/ADV056 - list() uses Specifications + filter query
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;

    public TradeService(TradeRepository tradeRepo,
            CounterpartyRepository cpRepo,
            InstrumentRepository instRepo,
            TradeEventProducer events,
            TradeMetrics metrics) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
    }

    public Trade create(TradeRequest req, String actor) {
        tradeRepo.findByTradeRef(req.tradeRef()).ifPresent(existing -> {
            throw new DuplicateTradeRefException(req.tradeRef());
        });

        Trade trade = new Trade();
        trade.setTradeRef(req.tradeRef());
        trade.setInstrument(instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException("Instrument " + req.instrumentId())));
        trade.setCounterparty(cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException("Counterparty " + req.counterpartyId())));
        trade.setAssetClass(req.assetClass());
        trade.setSide(req.side());
        trade.setQuantity(req.quantity());
        trade.setPrice(req.price());
        trade.setTradeDate(req.tradeDate());
        trade.setStatus(TradeStatus.PENDING);

        Trade saved = tradeRepo.save(trade);
        metrics.incrementTradeCreated();
        metrics.recordTradeValue(saved.getQuantity().multiply(saved.getPrice()).doubleValue());
        events.publish(new TradeEvent(
                UUID.randomUUID(),
                saved.getTradeRef(),
                TradeEvent.EventType.TRADE_CREATED,
                Instant.now(),
                actor,
                null,
                saved.getStatus().name()));
        return saved;
    }

    public Trade update(Long id, TradeRequest req, String actor) {
        // TODO(TICKET-ADV065): load by id (throw TradeNotFoundException if missing),
        // copy mutable fields from req, save, publish a TRADE_UPDATED event.
        throw new UnsupportedOperationException("TICKET-ADV065");
    }

    public Trade updateStatus(Long id, String status, String actor) {
        Trade trade = tradeRepo.findById(id).orElseThrow(() -> new TradeNotFoundException(String.valueOf(id)));
        String before = trade.getStatus().name();
        TradeStatus newStatus = TradeStatus.valueOf(status);
        trade.setStatus(newStatus);
        Trade saved = tradeRepo.save(trade);
        events.publish(new TradeEvent(
                UUID.randomUUID(),
                saved.getTradeRef(),
                TradeEvent.EventType.TRADE_UPDATED,
                Instant.now(),
                actor,
                before,
                saved.getStatus().name()));
        return saved;
    }

    public void softDelete(Long id, String actor) {
        Trade trade = tradeRepo.findById(id).orElseThrow(() -> new TradeNotFoundException(String.valueOf(id)));

        trade.softDelete();
        Trade saved = tradeRepo.save(trade);
        events.publish(new TradeEvent(
                UUID.randomUUID(),
                saved.getTradeRef(),
                TradeEvent.EventType.TRADE_CANCELLED,
                Instant.now(),
                actor,
                null,
                null));
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(LocalDate from,
                            LocalDate to,
                            String status,
                            Long counterpartyId,
                            String counterparty,
                            Pageable pageable) {
        TradeStatus tradeStatus = status == null ? null : TradeStatus.valueOf(status);
        Specification<Trade> spec = Specification
                .where(tradeDateBetween(from, to))
                .and(hasStatus(tradeStatus))
                .and(forCounterparty(counterpartyId))
                .and(forCounterpartyName(counterparty));
        return tradeRepo.findAll(spec, pageable);
    }
}
