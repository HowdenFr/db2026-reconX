package com.dbtraining.reconx.repository.entity;

import com.dbtraining.reconx.dto.TradeEvent;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "kafka_partition", nullable = false)
    private Integer partition;

    @Column(name = "kafka_offset", nullable = false)
    private Long offset;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private TradeEvent payload;

    @Lob
    @Column(name = "reason")
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    public DlqMessage() {
    }

    public DlqMessage(UUID eventId,
                      String tradeRef,
                      String originalTopic,
                      Integer partition,
                      Long offset,
                      TradeEvent payload,
                      String reason,
                      Instant firstSeen) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.payload = payload;
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public Integer getPartition() {
        return partition;
    }

    public Long getOffset() {
        return offset;
    }

    public TradeEvent getPayload() {
        return payload;
    }

    public String getReason() {
        return reason;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }
}
