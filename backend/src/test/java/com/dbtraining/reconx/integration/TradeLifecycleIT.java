package com.dbtraining.reconx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TICKET-ADV078 — Full trade lifecycle over real HTTP, against real Postgres.
 *
 * Drives POST /trades -> GET /trades -> PATCH /status -> POST /recon/run ->
 * PUT /recon/results/{id}/resolve as admin@db.com, proving Liquibase + JWT +
 * RBAC + every controller from ADV063-074 wire together end to end.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

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
        // The default (dev) profile hardcodes the H2 driver and dialect; override
        // both since we're actually running against real Postgres here.
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @LocalServerPort private int port;

    private static String token;
    private static Long createdId;
    private static String reconJobId;
    private static final Long BREAK_ID = 1L;

    private final RestTemplate http = new RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    @Order(1)
    void loginAsAdmin() {
        String body = """
                {"email":"admin@db.com","password":"admin123"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var resp = http.postForEntity(url("/auth/login"), new HttpEntity<>(body, headers), JsonNode.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        token = resp.getBody().get("token").asText();
        assertNotNull(token);
    }

    @Test
    @Order(2)
    void createTrade() {
        // tradeRef regex: ^[A-Z]{3}-\d{8}-\d{4}$. assetClass and side are @NotBlank on
        // TradeRequest; status is server-side and must NOT appear in the request body.
        String body = """
                {"tradeRef":"INT-20260315-0001","instrumentId":1,"counterpartyId":1,
                 "assetClass":"EQUITY","side":"BUY",
                 "quantity":100.0,"price":245.50,"tradeDate":"2026-03-15"}
                """;

        var resp = http.exchange(url("/v1/trades"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), JsonNode.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        createdId = resp.getBody().get("id").asLong();
    }

    @Test
    @Order(3)
    void getTradeBack() {
        var resp = http.exchange(url("/v1/trades?status=PENDING"), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), JsonNode.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().get("totalElements").asLong() >= 1);
    }

    @Test
    @Order(4)
    void patchStatus() {
        String body = """
                {"status":"MATCHED"}
                """;

        var resp = http.exchange(url("/v1/trades/" + createdId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders()), JsonNode.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("MATCHED", resp.getBody().get("status").asText());
    }

    @Test
    @Order(5)
    void triggerRecon() {
        String body = """
                {"from":"2026-03-01","to":"2026-03-31"}
                """;

        var resp = http.exchange(url("/v1/recon/run"), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), JsonNode.class);

        assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());
        reconJobId = resp.getBody().get("jobId").asText();
        assertNotNull(reconJobId);
    }

    @Test
    @Order(6)
    void resolveBreak() {
        // Seeded by 008-seed.xml (008-seed-recon-break) so this step doesn't
        // depend on the recon engine actually detecting a break.
        String body = """
                {"note":"Confirmed via counterparty email on 2026-03-16."}
                """;

        var resp = http.exchange(url("/v1/recon/results/" + BREAK_ID + "/resolve"), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()), JsonNode.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("RESOLVED", resp.getBody().get("status").asText());
    }
}
