# Claude Prompt - ADR-0003

## User Prompt

You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: Prefer a GIN index with `jsonb_path_ops` over B-tree for
JSONB containment queries on `instruments.metadata`.

Alternatives we considered:
- B-tree index on extracted metadata keys.
- GIN with `jsonb_ops`.
- GIN with `jsonb_path_ops`.

Constraints / forces:
- Dominant predicate pattern is JSON containment (`@>`).
- Index size and write overhead must stay manageable.
- Must use PostgreSQL 16 native JSONB indexing support.

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: 2026-07-27" line.

## Notes

- Output curated into: `docs/adr/0003-gin-over-btree.md`