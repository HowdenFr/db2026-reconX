package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.ReconResultRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        // The default (dev) profile hardcodes H2Dialect; override it since we're
        // actually running against real Postgres here.
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private CounterpartyRepository counterpartyRepository;
    @Autowired private InstrumentRepository instrumentRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private ReconciliationService reconciliationService;
    @Autowired private ReconResultRepository reconResultRepository;

    @Test
    void containerIsRunning() {
        // sanity: if this passes, all your wiring is correct.
        // The real assertions live in TICKET-ADV045.
    }

    /**
     * TICKET-ADV045 — insert -> recon -> verify, against real Postgres.
     *
     * NOTE: the trades table enforces a UNIQUE trade_ref, so the internal and
     * external rows are persisted with distinct refs (two real, independent
     * rows — proving the SQL round-trip); the shared *business* tradeRef used
     * for matching is applied when each row is mapped to its domain
     * TradeType below, since that key is what ReconciliationEngine actually
     * matches on.
     */
    @Test
    @Transactional
    void insertedTradesAreReconciledAndPersisted() {
        // given
        Counterparty counterparty = new Counterparty();
        counterparty.setName("Test Bank AG");
        counterparty.setLeiCode("TESTLEI0000000000001");
        counterparty.setRegion("EU");
        counterpartyRepository.save(counterparty);

        Instrument instrument = new Instrument();
        instrument.setSymbol("SAP.DE");
        instrument.setName("SAP SE");
        instrument.setAssetClass("EQUITY");
        instrument.setCurrency("EUR");
        instrumentRepository.save(instrument);

        Trade internalRow = new Trade();
        internalRow.setTradeRef("EQU-20260728-0001");
        internalRow.setInstrument(instrument);
        internalRow.setCounterparty(counterparty);
        internalRow.setAssetClass("EQUITY");
        internalRow.setSide("BUY");
        internalRow.setQuantity(new BigDecimal("1000"));
        internalRow.setPrice(new BigDecimal("100.00"));
        internalRow.setTradeDate(LocalDate.of(2026, 7, 28));
        tradeRepository.save(internalRow);

        Trade externalRow = new Trade();
        externalRow.setTradeRef("EQU-20260728-0002");
        externalRow.setInstrument(instrument);
        externalRow.setCounterparty(counterparty);
        externalRow.setAssetClass("EQUITY");
        externalRow.setSide("BUY");
        externalRow.setQuantity(new BigDecimal("1000"));
        externalRow.setPrice(new BigDecimal("100.00"));
        externalRow.setTradeDate(LocalDate.of(2026, 7, 28));
        tradeRepository.save(externalRow);

        String sharedTradeRef = "EQU-20260728-0001";
        EquityTrade internal = toEquityTrade(internalRow, sharedTradeRef);
        EquityTrade external = toEquityTrade(externalRow, sharedTradeRef);

        // when
        reconciliationService.runRecon(List.of(internal), List.of(external), ReconciliationRule.EXACT);

        // then
        List<ReconResult> persisted = reconResultRepository.findAll();
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
        assertThat(persisted.get(0).tradeRef()).isEqualTo(sharedTradeRef);
    }

    private EquityTrade toEquityTrade(Trade trade, String businessTradeRef) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(businessTradeRef))
                .instrumentSymbol(trade.getInstrument().getSymbol())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .currency(trade.getInstrument().getCurrency())
                .side(Side.valueOf(trade.getSide()))
                .tradeDate(trade.getTradeDate())
                .counterpartyId(trade.getCounterparty().getId())
                .build();
    }
}
