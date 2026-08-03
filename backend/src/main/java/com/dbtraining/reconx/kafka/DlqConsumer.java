package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;

    public DlqConsumer(DlqMessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(
            topics = KafkaTopicsConfig.TRADE_EVENTS_DLQ,
            groupId = "dlq-monitor",
            containerFactory = "tradeEventListenerContainerFactory"
    )
    @Transactional
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(value = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String originalTopicHeader,
                             @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exMsg) {
        TradeEvent event = record.value();
        String originalTopic = originalTopicHeader != null ? originalTopicHeader : KafkaTopicsConfig.TRADE_EVENTS;
        String reason = exMsg != null ? exMsg : "unknown";

        log.error(
                "DLQ message captured eventId={} tradeRef={} originalTopic={} partition={} offset={} reason={} payload={}",
                event.eventId(),
                event.tradeRef(),
                originalTopic,
                record.partition(),
                record.offset(),
                reason,
                event
        );

        repo.save(new DlqMessage(
                event.eventId(),
                event.tradeRef(),
                originalTopic,
                record.partition(),
                record.offset(),
                event,
                reason,
                Instant.now()
        ));
    }
}
