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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.forCounterparty;
import static com.dbtraining.reconx.repository.TradeSpecifications.hasStatus;
import static com.dbtraining.reconx.repository.TradeSpecifications.tradeDateBetween;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV056 — exercises "only date range supplied" and "all filters
 * supplied" scenarios against the composed Specifications.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
class TradeSpecificationsTest {

    @Autowired private CounterpartyRepository counterpartyRepository;
    @Autowired private InstrumentRepository instrumentRepository;
    @Autowired private TradeRepository tradeRepository;

    @Test
    void onlyDateRangeSupplied_returnsAllTradesInRange() {
        Counterparty cpA = counterpartyRepository.findById(1L).orElseThrow();
        Counterparty cpB = counterpartyRepository.findById(2L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        trade("EQU-20260729-0010", instrument, cpA, TradeStatus.MATCHED);
        trade("EQU-20260729-0011", instrument, cpB, TradeStatus.PENDING);

        Specification<Trade> spec = Specification.where(
                tradeDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));

        Page<Trade> result = tradeRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Trade::getTradeRef)
                .contains("EQU-20260729-0010", "EQU-20260729-0011");
    }

    @Test
    void allFiltersSupplied_narrowsToMatchingTrade() {
        Counterparty cpA = counterpartyRepository.findById(3L).orElseThrow();
        Counterparty cpB = counterpartyRepository.findById(4L).orElseThrow();
        Instrument instrument = instrumentRepository.findById(1L).orElseThrow();

        Trade target = trade("EQU-20260729-0012", instrument, cpA, TradeStatus.MATCHED);
        trade("EQU-20260729-0013", instrument, cpA, TradeStatus.PENDING);
        trade("EQU-20260729-0014", instrument, cpB, TradeStatus.MATCHED);

        Specification<Trade> spec = Specification
                .where(tradeDateBetween(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .and(hasStatus(TradeStatus.MATCHED))
                .and(forCounterparty(cpA.getId()));

        Page<Trade> result = tradeRepository.findAll(spec, PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Trade::getTradeRef)
                .containsExactly(target.getTradeRef());
    }
    private Trade trade(String ref, Instrument instrument, Counterparty counterparty, TradeStatus status) {
        Trade t = new Trade();
        t.setTradeRef(ref);
        t.setInstrument(instrument);
        t.setCounterparty(counterparty);
        t.setAssetClass("EQUITY");
        t.setSide("BUY");
        t.setQuantity(new BigDecimal("10"));
        t.setPrice(new BigDecimal("100.00"));
        t.setTradeDate(LocalDate.of(2026, 7, 29));
        t.setStatus(status);
        return tradeRepository.save(t);
    }
}
