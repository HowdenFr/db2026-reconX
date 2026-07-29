package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:reconx-audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.test.database.replace=none"
})
@Import(TradeHistoryService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class TradeHistoryServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TradeHistoryService historyService;

    @Test
    void recordsInsertAndThreeUpdatesAndReturnsHistoricalSnapshots() {
        jdbcTemplate.execute("ALTER TABLE instruments ADD COLUMN metadata JSON");

        Long tradeId = inNewTransaction(() -> {
            Counterparty counterparty = entityManager.find(Counterparty.class, 1L);
            Instrument instrument = entityManager.find(Instrument.class, 1L);

            Trade trade = new Trade();
            trade.setTradeRef("AUD-20260729-0001");
            trade.setCounterparty(counterparty);
            trade.setInstrument(instrument);
            trade.setAssetClass("EQUITY");
            trade.setSide("BUY");
            trade.setQuantity(new BigDecimal("10.0000"));
            trade.setPrice(new BigDecimal("100.0000"));
            trade.setTradeDate(LocalDate.of(2026, 7, 29));
            entityManager.persist(trade);
            entityManager.flush();
            return trade.getId();
        });

        updatePrice(tradeId, "101.0000");
        updatePrice(tradeId, "102.0000");
        updatePrice(tradeId, "103.0000");

        List<Number> revisions = historyService.revisionsFor(tradeId);

        assertThat(revisions).hasSize(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trades_aud WHERE id = ?",
                Integer.class,
                tradeId)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revinfo WHERE rev IN (?, ?, ?, ?)",
                Integer.class,
                revisions.get(0), revisions.get(1), revisions.get(2), revisions.get(3)))
                .isEqualTo(4);

        Trade firstUpdate = historyService.snapshotAt(tradeId, revisions.get(1));
        assertThat(firstUpdate.getPrice()).isEqualByComparingTo("101.0000");
    }

    private void updatePrice(Long tradeId, String price) {
        inNewTransaction(() -> {
            Trade trade = entityManager.find(Trade.class, tradeId);
            trade.setPrice(new BigDecimal(price));
            entityManager.flush();
            return null;
        });
    }

    private <T> T inNewTransaction(java.util.function.Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> work.get());
    }
}
