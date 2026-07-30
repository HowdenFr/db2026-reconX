package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.exception.GlobalExceptionHandler;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void list_returnsPagedEnvelopeAndPassesFiltersAndSort() throws Exception {
        Trade trade = new Trade();
        trade.setTradeRef("EQU-20260729-1001");
        trade.setStatus(TradeStatus.PENDING);

        TradeResponse response = new TradeResponse(
                1L,
                "EQU-20260729-1001",
                10L,
                "SAP.DE",
                20L,
                "Apex Clearing",
                "EQUITY",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                LocalDate.of(2026, 7, 29),
                "PENDING",
                Instant.parse("2026-07-29T12:00:00Z"),
                Instant.parse("2026-07-29T12:05:00Z"));

        Page<Trade> page = new PageImpl<>(
                List.of(trade),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"))),
                1);

        when(tradeService.list(any(), any(), anyString(), anyLong(), any(Pageable.class)))
                .thenReturn(page);
        when(tradeMapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(get("/v1/trades")
                        .param("status", "PENDING")
                        .param("counterpartyId", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tradeRef").value("EQU-20260729-1001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tradeService).list(eq(null), eq(null), eq("PENDING"), eq(20L), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void list_withOnlyStatusFilterStillReturnsOk() throws Exception {
        when(tradeService.list(any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20, Sort.by(Sort.Order.desc("tradeDate")))));

        mockMvc.perform(get("/v1/trades").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
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