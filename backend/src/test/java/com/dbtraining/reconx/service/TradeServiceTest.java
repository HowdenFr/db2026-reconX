package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock private TradeRepository tradeRepo;
    @Mock private CounterpartyRepository cpRepo;
    @Mock private InstrumentRepository instRepo;
    @Mock private TradeEventProducer events;
    @Mock private TradeMetrics metrics;

    @InjectMocks private TradeService tradeService;

    @Test
    void create_persistsPendingTradeAndPublishesEvent() throws Exception {
        TradeRequest request = new TradeRequest(
                "TRD-20260315-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0"),
                new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15));

        Instrument instrument = new Instrument();
        instrument.setSymbol("SAP.DE");
        Counterparty counterparty = new Counterparty();
        counterparty.setName("Apex Clearing");
        counterparty.setLeiCode("TESTLEI1234567890001");
        counterparty.setRegion("NAMR");

        when(tradeRepo.findByTradeRef("TRD-20260315-0001")).thenReturn(Optional.empty());
        when(instRepo.findById(1L)).thenReturn(Optional.of(instrument));
        when(cpRepo.findById(2L)).thenReturn(Optional.of(counterparty));
        doAnswer(invocation -> {
            Trade trade = invocation.getArgument(0);
            setId(trade, 42L);
            return trade;
        }).when(tradeRepo).save(any(Trade.class));

        Trade saved = tradeService.create(request, "alice");

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getTradeRef()).isEqualTo("TRD-20260315-0001");
        assertThat(saved.getStatus()).isEqualTo(TradeStatus.PENDING);
        assertThat(saved.getInstrument()).isSameAs(instrument);
        assertThat(saved.getCounterparty()).isSameAs(counterparty);

        verify(metrics).incrementTradeCreated();
        verify(metrics).recordTradeValue(24550.0d);

        ArgumentCaptor<TradeEvent> eventCaptor = ArgumentCaptor.forClass(TradeEvent.class);
        verify(events).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tradeRef()).isEqualTo("TRD-20260315-0001");
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(TradeEvent.EventType.TRADE_CREATED);
        assertThat(eventCaptor.getValue().actor()).isEqualTo("alice");
        assertThat(eventCaptor.getValue().before()).isNull();
        assertThat(eventCaptor.getValue().after()).isEqualTo("PENDING");
    }

    @Test
    void create_duplicateTradeRef_throwsConflictException() {
        Trade existing = new Trade();
        existing.setTradeRef("TRD-20260315-0001");

        when(tradeRepo.findByTradeRef("TRD-20260315-0001")).thenReturn(Optional.of(existing));

        TradeRequest request = new TradeRequest(
                "TRD-20260315-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0"),
                new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15));

        assertThatThrownBy(() -> tradeService.create(request, "alice"))
                .isInstanceOf(DuplicateTradeRefException.class)
                .hasMessage("Duplicate tradeRef: TRD-20260315-0001");

        verify(tradeRepo, never()).save(any(Trade.class));
        verify(events, never()).publish(any(TradeEvent.class));
    }

    private static void setId(Trade trade, Long id) throws Exception {
        Field field = Trade.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(trade, id);
    }
}
