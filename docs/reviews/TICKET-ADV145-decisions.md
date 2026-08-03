# TICKET-ADV145 — Kafka consumer config review

Review prompt: [`TICKET-ADV145-prompt.md`](./TICKET-ADV145-prompt.md), run against the
actual current `application.yml` (Kafka section) and `KafkaErrorHandlerConfig.java`.

| # | Area | Finding | Recommendation | Decision | Rationale |
|---|------|---------|-----------------|----------|-----------|
| 1 | Idempotence | `ReconciliationConsumer` has no dedup guard against redelivery (e.g. after a rebalance with an uncommitted offset) — unlike `AuditEventConsumer`, which gets natural idempotency from `audit_log.event_id`'s unique constraint. A redelivered message would double-schedule a recon. | Add an `event_id`-based dedup check, or accept the risk. | **Accept** (documented, not fully fixed) | Real, asymmetric gap. Recon scheduling is idempotent-ish in effect (re-running recon on an already-reconciled trade is harmless), so we documented the gap in code rather than building a dedup store today. |
| 2 | Error handling | `ExponentialBackOff(1000L, 2.0)` has no jitter — a burst of failures (e.g. broker blip) retries in lockstep across all partitions/consumers. | Add jitter via a custom `BackOff`. | **Defer** | Real gap, low blast radius at current scale (~500 events/sec, single broker in dev). Backlog item. |
| 3 | Error handling | `IllegalArgumentException` is globally non-retryable, not scoped to deserialization specifically. | Scope non-retryable exceptions more narrowly. | **Reject** | Checked call sites: in these three consumers, `IllegalArgumentException` only ever originates from malformed enum/data parsing, so treating it as non-retryable is actually correct here, not overly broad. |
| 4 | Observability | Kafka consumer log lines carried no correlation identifier — `MdcFilter` only covers the HTTP filter chain, not consumer threads, so a single event couldn't be traced across consumers by ID. | Propagate `tradeRef` into MDC at the top of each listener method (reusing the existing `[correlationId] [tradeRef]` log pattern). | **Accept** | Cheap, and the value was already available in every `TradeEvent`. Implemented in `ReconciliationConsumer` and `AuditEventConsumer`; verified live — log lines now carry the tradeRef where they previously had nothing. |
| 5 | Idempotence | Producer `enable.idempotence` was never explicitly asserted — relies on the Kafka 3.x client default. | Set `spring.kafka.producer.properties.enable.idempotence: true` explicitly. | **Accept** | Cheap insurance against a future client-library default change silently turning this off. Verified live — producer still publishes correctly with the flag set. |
| 6 | Security | `bootstrap-servers` is PLAINTEXT, no SASL/SSL/ACLs. | Use SASL_SSL in prod. | **Reject** (for now) | Known dev-only gap; this is explicitly Day 10 infra scope, not this ticket's. |

Note: the review also considered the commonly-cited "missing `spring.application.name`
metric tag" finding — checked and **not actually present**; `management.metrics.tags.application`
was already set in ADV139. Not listed as an open finding since it's already resolved.

**Decision mix**: 3 accept, 1 defer, 2 reject — not a rubber stamp.
