// ...existing code...
package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class TradeAggregator {

    private static final Logger log = LoggerFactory.getLogger(TradeAggregator.class);
    private final AuditLogRepository auditRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TradeAggregator(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;
        for (AuditLogEntry e : events) {
            String et = e.getEventType();
            try {
                TradeEvent.EventType eventType = TradeEvent.EventType.valueOf(et);
                switch (eventType) {
                    case TRADE_CREATED, TRADE_UPDATED -> {
                        String after = e.getAfterState();
                        if (after != null && !after.isBlank()) {
                            try {
                                state = mapper.readTree(after);
                            } catch (IOException je) {
                                log.warn("Failed to parse afterState for audit id {}: {}", e.getId(), je.getMessage());
                                // continue to next event
                            }
                        } // else keep previous state
                    }
                    case TRADE_CANCELLED -> state = null;
                    default -> log.debug("Unhandled audit event type: {}", et);
                }
            } catch (IllegalArgumentException iae) {
                log.warn("Unknown event type in audit row id {}: {}", e.getId(), et);
            }
        }
        return Optional.ofNullable(state);
    }
}