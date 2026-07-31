package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditLogRepository repo;

    @Test
    void tradeCreatedPersistsAuditRowWithNullBefore() {
        var after = JsonNodeFactory.instance.objectNode().put("status", "PENDING");
        TradeEvent event = TradeEvent.created("TRD-20260731-0001", after);

        new AuditEventConsumer(repo).onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repo).save(captor.capture());
        AuditLogEntry saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo(event.eventId().toString());
        assertThat(saved.getTradeRef()).isEqualTo("TRD-20260731-0001");
        assertThat(saved.getEventType()).isEqualTo("TRADE_CREATED");
        assertThat(saved.getEventTimestamp()).isEqualTo(event.timestamp());
        assertThat(saved.getBeforeState()).isNull();
        assertThat(saved.getAfterState()).isEqualTo(after.toString());
    }

    @Test
    void tradeCancelledPersistsAuditRowWithNullAfter() {
        var before = JsonNodeFactory.instance.objectNode().put("status", "PENDING");
        TradeEvent event = TradeEvent.cancelled("TRD-20260731-0002", before);

        new AuditEventConsumer(repo).onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repo).save(captor.capture());
        AuditLogEntry saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo("TRADE_CANCELLED");
        assertThat(saved.getBeforeState()).isEqualTo(before.toString());
        assertThat(saved.getAfterState()).isNull();
    }

    @Test
    void jacksonNullNodeIsStoredAsNullNotTheStringNull() {
        // A JSON-null field round-trips through Kafka as a Jackson NullNode,
        // not Java null -- this is what created()/cancelled() with a literal
        // null argument does NOT reproduce, since that stays Java null in
        // memory. Simulate the real post-deserialization shape directly.
        var nullNode = JsonNodeFactory.instance.nullNode();
        TradeEvent event = new TradeEvent(
                java.util.UUID.randomUUID(), "TRD-20260731-0003",
                TradeEvent.EventType.TRADE_CREATED, java.time.Instant.now(),
                nullNode, nullNode);

        new AuditEventConsumer(repo).onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(repo).save(captor.capture());
        AuditLogEntry saved = captor.getValue();

        assertThat(saved.getBeforeState()).isNull();
        assertThat(saved.getAfterState()).isNull();
    }
}
