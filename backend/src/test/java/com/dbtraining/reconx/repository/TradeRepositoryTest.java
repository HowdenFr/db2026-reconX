package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV055 — smoke test for TradeRepository.findByFilters.
 *
 * NOTE: 008-seed.xml only loads counterparties/instruments/users, not
 * trades (its header comment claiming "trades -> 500" doesn't match what
 * it actually does) — so this test inserts its own trade rather than
 * relying on seed data.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class TradeRepositoryTest {

    @Autowired private CounterpartyRepository counterpartyRepository;
    @Autowired private InstrumentRepository instrumentRepository;
    @Autowired private TradeRepository tradeRepository;

    @Test
    void findByFilters_dateRangeOnly_returnsTradesWithinRange() {
        Counterparty counterparty = counterpartyRepository.findById(1L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade trade = new Trade();
        trade.setTradeRef("EQU-20260729-0001");
        trade.setInstrument(instrument);
        trade.setCounterparty(counterparty);
        trade.setAssetClass("EQUITY");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("1000"));
        trade.setPrice(new BigDecimal("100.00"));
        trade.setTradeDate(LocalDate.of(2026, 7, 29));
        tradeRepository.save(trade);

        Page<Trade> result = tradeRepository.findByFilters(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null,
                null,
                PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Trade::getTradeRef)
                .contains("EQU-20260729-0001");
    }

    @Test
    void findByFilters_statusAndCounterpartyFilters_narrowResults() {
        Counterparty cpA = counterpartyRepository.findById(1L).orElseThrow();
        Counterparty cpB = counterpartyRepository.findById(2L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade matched = new Trade();
        matched.setTradeRef("EQU-20260729-0002");
        matched.setInstrument(instrument);
        matched.setCounterparty(cpA);
        matched.setAssetClass("EQUITY");
        matched.setSide("BUY");
        matched.setQuantity(new BigDecimal("10"));
        matched.setPrice(new BigDecimal("50.00"));
        matched.setTradeDate(LocalDate.of(2026, 7, 29));
        matched.setStatus(TradeStatus.MATCHED);
        tradeRepository.save(matched);

        Trade pendingOtherCp = new Trade();
        pendingOtherCp.setTradeRef("EQU-20260729-0003");
        pendingOtherCp.setInstrument(instrument);
        pendingOtherCp.setCounterparty(cpB);
        pendingOtherCp.setAssetClass("EQUITY");
        pendingOtherCp.setSide("SELL");
        pendingOtherCp.setQuantity(new BigDecimal("20"));
        pendingOtherCp.setPrice(new BigDecimal("51.00"));
        pendingOtherCp.setTradeDate(LocalDate.of(2026, 7, 29));
        tradeRepository.save(pendingOtherCp);

        Page<Trade> result = tradeRepository.findByFilters(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                TradeStatus.MATCHED,
                cpA.getId(),
                PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Trade::getTradeRef)
                .containsExactly("EQU-20260729-0002");
    }
}
