package com.dbtraining.reconx.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeEventTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void updatedFactoryCreatesFreshMetadataAndRoundTripsSnapshots() throws Exception {
        JsonNode before = objectMapper.readTree("""
                {"status":"PENDING","quantity":10}
                """);
        JsonNode after = objectMapper.readTree("""
                {"status":"MATCHED","quantity":10}
                """);

        TradeEvent first = TradeEvent.updated("TRD-20260731-0001", before, after);
        TradeEvent second = TradeEvent.updated("TRD-20260731-0001", before, after);
        TradeEvent roundTripped = objectMapper.readValue(
                objectMapper.writeValueAsBytes(first), TradeEvent.class);

        assertThat(first.eventId()).isNotNull().isNotEqualTo(second.eventId());
        assertThat(first.timestamp()).isNotNull();
        assertThat(first.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(roundTripped).isEqualTo(first);
        assertThat(roundTripped.before()).isEqualTo(before);
        assertThat(roundTripped.after()).isEqualTo(after);
    }

    @Test
    void factoriesSetAbsentSnapshotsToNull() {
        JsonNode snapshot = objectMapper.createObjectNode().put("status", "PENDING");

        TradeEvent created = TradeEvent.created("TRD-20260731-0002", snapshot);
        TradeEvent cancelled = TradeEvent.cancelled("TRD-20260731-0002", snapshot);

        assertThat(created.before()).isNull();
        assertThat(created.after()).isEqualTo(snapshot);
        assertThat(cancelled.before()).isEqualTo(snapshot);
        assertThat(cancelled.after()).isNull();
    }
}
