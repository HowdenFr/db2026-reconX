package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import com.dbtraining.reconx.repository.entity.DlqMessage;
import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqAdminController.class)
@Import(DlqAdminControllerWebMvcTest.TestSecurityConfig.class)
class DlqAdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DlqMessageRepository repo;

    @MockBean
    private TradeEventProducer producer;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, exception) ->
                                    response.sendError(HttpStatus.UNAUTHORIZED.value())));
            return http.build();
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listReturnsPersistedDlqMessages() throws Exception {
        DlqMessage message = sampleMessage();
        when(repo.findAllByOrderByFirstSeenAsc()).thenReturn(java.util.List.of(message));

        mockMvc.perform(get("/v1/admin/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(message.getEventId().toString()))
                .andExpect(jsonPath("$[0].tradeRef").value(message.getTradeRef()))
                .andExpect(jsonPath("$[0].originalTopic").value("trade-events"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replayDryRunReturnsPreviewWithoutPublishingOrDeleting() throws Exception {
        DlqMessage message = sampleMessage();
        when(repo.findByEventId(message.getEventId())).thenReturn(Optional.of(message));

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .with(csrf())
                        .queryParam("eventId", message.getEventId().toString())
                        .queryParam("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.wouldReplayTo").value("trade-events"))
                .andExpect(jsonPath("$.tradeRef").value(message.getTradeRef()));

        verifyNoInteractions(producer);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replayPublishesPayloadAndDeletesDlqRow() throws Exception {
        DlqMessage message = sampleMessage();
        when(repo.findByEventId(message.getEventId())).thenReturn(Optional.of(message));

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .with(csrf())
                        .queryParam("eventId", message.getEventId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.eventId").value(message.getEventId().toString()))
                .andExpect(jsonPath("$.topic").value("trade-events"));

        verify(producer).publish(message.getPayload());
        verify(repo).delete(message);
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void replayAsTraderReturnsForbidden() throws Exception {
        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .with(csrf())
                        .queryParam("eventId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void replayUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .queryParam("eventId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replayMissingMessageReturnsNotFound() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/v1/admin/dlq/replay")
                        .with(csrf())
                        .queryParam("eventId", eventId.toString()))
                .andExpect(status().isNotFound());
    }

    private static DlqMessage sampleMessage() {
        TradeEvent event = TradeEvent.created("TRD-20260731-0136", null);
        return new DlqMessage(
                event.eventId(),
                event.tradeRef(),
                "trade-events",
                1,
                99L,
                event,
                "boom",
                Instant.parse("2026-07-31T16:30:00Z")
        );
    }
}
