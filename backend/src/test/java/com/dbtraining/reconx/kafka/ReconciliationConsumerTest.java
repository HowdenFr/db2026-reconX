package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReconciliationConsumerTest {

    @Mock
    private ReconciliationEngine reconEngine;

    @Test
    void tradeCreatedSchedulesRecon() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0001", null);

        new ReconciliationConsumer(reconEngine).onTradeEvent(event);

        verify(reconEngine).scheduleRecon("TRD-20260731-0001");
        verify(reconEngine, never()).cancelPendingRecon(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tradeUpdatedSchedulesRecon() {
        TradeEvent event = TradeEvent.updated("TRD-20260731-0002", null, null);

        new ReconciliationConsumer(reconEngine).onTradeEvent(event);

        verify(reconEngine).scheduleRecon("TRD-20260731-0002");
    }

    @Test
    void tradeCancelledCancelsPendingRecon() {
        TradeEvent event = TradeEvent.cancelled("TRD-20260731-0003", null);

        new ReconciliationConsumer(reconEngine).onTradeEvent(event);

        verify(reconEngine).cancelPendingRecon("TRD-20260731-0003");
        verify(reconEngine, never()).scheduleRecon(org.mockito.ArgumentMatchers.anyString());
    }
}
