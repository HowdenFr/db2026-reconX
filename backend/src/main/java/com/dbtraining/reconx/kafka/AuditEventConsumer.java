package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * TICKET-ADV132 — AuditEventConsumer
 *
 * WHAT:    Persists every TradeEvent flowing through `trade-events` into the
 *          audit_log table.
 * HOW:     @KafkaListener on `trade-events`, groupId `audit-service`. Maps
 *          the TradeEvent DTO -> AuditLogEntry entity -> repo.save(...).
 * WHY:     Together with ADV137 this powers event-sourced replay — every
 *          domain change is captured immutably.
 * OBSERVE: After a POST /api/v1/trades, query audit_log -> one new row with
 *          the same eventId.
 * ============================================================================
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    private final AuditLogRepository repo;

    public AuditEventConsumer(AuditLogRepository repo) { this.repo = repo; }

    @KafkaListener(topics = "trade-events", groupId = "audit-service")
    @Transactional
    public void onTradeEvent(TradeEvent e) {
        try {
            MDC.put("tradeRef", e.tradeRef());
            repo.save(new AuditLogEntry(
                    e.eventId().toString(),
                    e.tradeRef(),
                    e.eventType().name(),
                    e.timestamp(),
                    null,
                    toText(e.before()),
                    toText(e.after())));
            log.debug("Audit row persisted for eventId={}", e.eventId());
        } finally {
            MDC.remove("tradeRef");
        }
    }

    // A JSON-null field round-trips through Kafka as a Jackson NullNode, not
    // Java null -- NullNode.toString() would otherwise store the literal
    // 4-character string "null" instead of an empty column.
    private static String toText(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.toString();
    }
}
