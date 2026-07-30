package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReconControllerTest {

    @Mock private ReconBreakRepository breaks;
    @Mock private ReconciliationService reconciliationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReconController controller = new ReconController(breaks, reconciliationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void runRecon_withBody_returnsAcceptedAndInvokesService() throws Exception {
        when(reconciliationService.runRecon(any(), any(), eq(com.dbtraining.reconx.model.ReconciliationRule.EXACT)))
                .thenReturn(List.of());

        mockMvc.perform(post("/v1/recon/run")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "from":"2026-07-01",
                                  "to":"2026-07-30",
                                  "counterpartyId":7
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isString())
                .andExpect(jsonPath("$.status").value("QUEUED"));

        ArgumentCaptor<List> internalCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> externalCaptor = ArgumentCaptor.forClass(List.class);
        verify(reconciliationService).runRecon(
                internalCaptor.capture(),
                externalCaptor.capture(),
                eq(com.dbtraining.reconx.model.ReconciliationRule.EXACT));

        assertThat(internalCaptor.getValue()).hasSize(48);
        assertThat(externalCaptor.getValue()).hasSize(48);
    }

    @Test
    void runRecon_withoutBody_usesDefaultRangeAndStillReturnsAccepted() throws Exception {
        when(reconciliationService.runRecon(any(), any(), eq(com.dbtraining.reconx.model.ReconciliationRule.EXACT)))
                .thenReturn(List.of());

        mockMvc.perform(post("/v1/recon/run"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isString())
                .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(reconciliationService).runRecon(any(), any(), eq(com.dbtraining.reconx.model.ReconciliationRule.EXACT));
    }
}
