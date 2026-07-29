package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock private TradeService tradeService;
    @Mock private TradeMapper tradeMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TradeController controller = new TradeController(tradeService, tradeMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void create_validRequest_returnsCreatedWithLocationHeader() throws Exception {
        Trade saved = new Trade();
        setId(saved, 42L);
        saved.setTradeRef("TRD-20260315-0001");

        TradeResponse response = new TradeResponse(
                42L,
                "TRD-20260315-0001",
                1L,
                "SAP.DE",
                2L,
                "Apex Clearing",
                "EQUITY",
                "BUY",
                new BigDecimal("100.0"),
                new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 15),
                "PENDING",
                Instant.parse("2026-03-15T10:15:30Z"),
                Instant.parse("2026-03-15T10:15:30Z"));

        when(tradeService.create(any(TradeRequest.class), anyString())).thenReturn(saved);
        when(tradeMapper.toResponse(saved)).thenReturn(response);

        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeRef":"TRD-20260315-0001",
                                  "instrumentId":1,
                                  "counterpartyId":2,
                                  "assetClass":"EQUITY",
                                  "side":"BUY",
                                  "quantity":100.0,
                                  "price":245.50,
                                  "tradeDate":"2026-03-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trades/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.tradeRef").value("TRD-20260315-0001"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(tradeService).create(any(TradeRequest.class), anyString());
        verify(tradeMapper).toResponse(saved);
    }

    @Test
    void create_invalidRequest_returnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instrumentId":1,
                                  "counterpartyId":2,
                                  "assetClass":"EQUITY",
                                  "side":"BUY",
                                  "quantity":-5,
                                  "price":245.50,
                                  "tradeDate":"2026-07-30"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("tradeRef"),
                        org.hamcrest.Matchers.containsString("quantity"),
                        org.hamcrest.Matchers.containsString("tradeDate"))));
    }

    private static void setId(Trade trade, Long id) throws Exception {
        Field field = Trade.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(trade, id);
    }
}
