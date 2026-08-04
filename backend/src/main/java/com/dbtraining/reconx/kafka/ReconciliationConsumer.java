package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * TICKET-ADV131 — ReconciliationConsumer
 *
 * WHAT:    Listens for `trade-events` and schedules a reconciliation job.
 * HOW:     @KafkaListener on `trade-events`, groupId `recon-service`. In the
 *          full implementation this would insert a row into recon_jobs and
 *          trigger the engine; the trainer reference logs the trigger so
 *          students can trace the message flow end-to-end.
 * WHY:     Decouples "trade saved" from "trade reconciled" so a slow recon
 *          run never blocks the trade-write path.
 * OBSERVE: A POST /api/v1/trades shows up here as a log line referencing the
 *          same eventId emitted by TradeEventProducer.
 *
 * TICKET-ADV145 (config review, finding #1): unlike AuditEventConsumer, this
 * listener has no dedup guard against redelivery (e.g. after a rebalance with
 * an uncommitted offset) -- a redelivered event would call scheduleRecon()
 * twice for the same tradeRef. Accepted as a known gap, not fixed here: recon
 * scheduling is naturally idempotent-ish (re-running recon on an
 * already-reconciled trade is harmless), so the cost of double-scheduling is
 * low relative to adding a dedup store right now.
 * ============================================================================
 */
@Component
public class ReconciliationConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationConsumer.class);

    private final ReconciliationEngine reconEngine;

    public ReconciliationConsumer(ReconciliationEngine reconEngine) {
        this.reconEngine = reconEngine;
    }

    @KafkaListener(topics = "trade-events", groupId = "recon-service")
    public void onTradeEvent(TradeEvent event) {
        try {
            MDC.put("tradeRef", event.tradeRef());
            log.debug("Recon-trigger received eventId={} ref={} type={}",
                    event.eventId(), event.tradeRef(), event.eventType());

            switch (event.eventType()) {
                case TRADE_CREATED, TRADE_UPDATED -> reconEngine.scheduleRecon(event.tradeRef());
                case TRADE_CANCELLED -> reconEngine.cancelPendingRecon(event.tradeRef());
            }
        } finally {
            MDC.remove("tradeRef");
        }
    }
}
