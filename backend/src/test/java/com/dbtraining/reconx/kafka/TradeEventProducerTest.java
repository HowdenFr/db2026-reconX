package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static com.dbtraining.reconx.kafka.KafkaTopicsConfig.TRADE_EVENTS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeEventProducerTest {

    @Mock
    private KafkaTemplate<String, TradeEvent> template;

    @Test
    void publishSendsAsynchronouslyToTradeEventsKeyedByTradeRef() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0001", null);
        CompletableFuture<SendResult<String, TradeEvent>> future = new CompletableFuture<>();
        when(template.send(TRADE_EVENTS, event.tradeRef(), event)).thenReturn(future);

        new TradeEventProducer(template).publish(event);

        verify(template).send(TRADE_EVENTS, event.tradeRef(), event);
    }

    @Test
    void publishHandlesAsynchronousFailureWithoutThrowing() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0002", null);
        CompletableFuture<SendResult<String, TradeEvent>> future =
                CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));
        when(template.send(TRADE_EVENTS, event.tradeRef(), event)).thenReturn(future);

        new TradeEventProducer(template).publish(event);

        verify(template).send(TRADE_EVENTS, event.tradeRef(), event);
    }
}
