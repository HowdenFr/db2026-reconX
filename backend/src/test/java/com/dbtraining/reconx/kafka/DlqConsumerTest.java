package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DlqConsumerTest {

    @Mock
    private DlqMessageRepository repo;

    @Test
    void dlqMessageIsPersistedWithOriginalTopicPartitionOffsetPayloadAndReason() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0136", null);
        ConsumerRecord<String, TradeEvent> record =
                new ConsumerRecord<>(KafkaTopicsConfig.TRADE_EVENTS_DLQ, 2, 45L, event.tradeRef(), event);

        new DlqConsumer(repo).onDlqMessage(record, KafkaTopicsConfig.TRADE_EVENTS, "boom");

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(repo).save(captor.capture());
        DlqMessage saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getTradeRef()).isEqualTo(event.tradeRef());
        assertThat(saved.getOriginalTopic()).isEqualTo(KafkaTopicsConfig.TRADE_EVENTS);
        assertThat(saved.getPartition()).isEqualTo(2);
        assertThat(saved.getOffset()).isEqualTo(45L);
        assertThat(saved.getPayload()).isEqualTo(event);
        assertThat(saved.getReason()).isEqualTo("boom");
        assertThat(saved.getFirstSeen()).isNotNull();
    }

    @Test
    void dlqMessageFallsBackToTradeEventsTopicAndUnknownReasonWhenHeadersMissing() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0137", null);
        ConsumerRecord<String, TradeEvent> record =
                new ConsumerRecord<>(KafkaTopicsConfig.TRADE_EVENTS_DLQ, 0, 3L, event.tradeRef(), event);

        new DlqConsumer(repo).onDlqMessage(record, null, null);

        ArgumentCaptor<DlqMessage> captor = ArgumentCaptor.forClass(DlqMessage.class);
        verify(repo).save(captor.capture());
        DlqMessage saved = captor.getValue();

        assertThat(saved.getOriginalTopic()).isEqualTo(KafkaTopicsConfig.TRADE_EVENTS);
        assertThat(saved.getReason()).isEqualTo("unknown");
    }
}
