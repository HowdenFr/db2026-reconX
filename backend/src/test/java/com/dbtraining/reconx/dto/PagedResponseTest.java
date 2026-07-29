package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagedResponseTest {

    @Test
    void of_mapsPageContentAndPaginationMetadata() {
        Trade first = trade("ref1");
        Trade second = trade("ref2");
        Page<Trade> page = new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 2);

        PagedResponse<String> response = PagedResponse.of(page, Trade::getTradeRef);

        assertThat(response.items()).containsExactly("ref1", "ref2");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.last()).isTrue();
    }

    private Trade trade(String tradeRef) {
        Counterparty counterparty = new Counterparty();
        counterparty.setName("Counterparty");
        counterparty.setLeiCode("LEI1234567890123456");
        counterparty.setRegion("EU");

        Instrument instrument = new Instrument();
        instrument.setSymbol("SAP.DE");
        instrument.setName("SAP SE");
        instrument.setAssetClass(Instrument.AssetClass.EQUITY);
        instrument.setCurrency("EUR");

        Trade trade = new Trade();
        trade.setTradeRef(tradeRef);
        trade.setCounterparty(counterparty);
        trade.setInstrument(instrument);
        trade.setAssetClass("EQUITY");
        trade.setSide("BUY");
        trade.setQuantity(new BigDecimal("10"));
        trade.setPrice(new BigDecimal("100.00"));
        trade.setTradeDate(LocalDate.of(2026, 7, 29));
        trade.setStatus(TradeStatus.PENDING);
        return trade;
    }
}
