# Claude Prompt - ADR-0002

## User Prompt

You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: Store non-canonical instrument attributes in
`instruments.metadata` using PostgreSQL `JSONB`.

Alternatives we considered:
- Fully normalized metadata tables (EAV style).
- Store JSON as plain `TEXT`.
- Use `JSONB`.

Constraints / forces:
- Flexible metadata changes without frequent DDL.
- Queryability for analyst investigations.
- Preserve strict relational integrity for canonical fields.

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: 2026-07-27" line.

## Notes

- Output curated into: `docs/adr/0002-jsonb-metadata.md`