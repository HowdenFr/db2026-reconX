Review the following Spring Kafka consumer configuration for production
readiness. Flag any missing or risky settings in these areas:
  (1) backpressure & poll tuning,
  (2) error handling, retry & DLQ,
  (3) idempotence and exactly-once semantics,
  (4) observability — metrics, logging, traces,
  (5) security — TLS, SASL, ACLs.

For each finding, give the concrete config key (or file/line), the
recommended value or fix, and a one-line justification. Do NOT rewrite the
whole file — just list findings.

Application context: trade reconciliation service, ~500 events/sec, strict
audit requirements. Three consumer groups (recon-service, audit-service,
alert-service) share one DLQ-backed error handler.

=== application.yml (spring.kafka section) ===
(pasted from backend/src/main/resources/application.yml)

=== KafkaErrorHandlerConfig.java ===
(pasted from backend/src/main/java/com/dbtraining/reconx/kafka/KafkaErrorHandlerConfig.java)
