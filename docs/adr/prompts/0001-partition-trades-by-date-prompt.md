# Claude Prompt - ADR-0001

## User Prompt

You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: Partition the `trades` table by `trade_date` using monthly
range partitions.

Alternatives we considered:
- Keep a single unpartitioned `trades` table with larger indexes.
- Partition by `counterparty_id`.
- Partition by `trade_date`.

Constraints / forces:
- Date-range filters dominate analyst and reconciliation queries.
- Retention/archive operations must be safe and fast.
- PostgreSQL 16 partitioning and key constraints apply.

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: 2026-07-27" line.

## Notes

- Output curated into: `docs/adr/0001-partition-trades-by-date.md`