# ADR-0003 - Use GIN (`jsonb_path_ops`) for JSONB containment queries

## Status

Accepted | Date: 2026-07-27

## Context

ReconX analysts frequently filter instrument metadata using containment-style
predicates (for example `metadata @> '{"sector":"Banking"}'`). With about
50,000 new trades/day and a 5-year horizon, metadata lookups must remain fast
for 10 concurrent analysts and scheduled reconciliation workloads.

Alternatives considered:
- B-tree index on extracted text expression(s).
- GIN default operator class (`jsonb_ops`).
- GIN `jsonb_path_ops` focused on containment.

Constraints/forces:
- Primary query shape is containment, not full JSON path search.
- Index storage and write overhead must remain bounded.
- Must work with PostgreSQL 16 native JSONB operator support.

## Decision

Create a GIN index using `jsonb_path_ops` on `instruments.metadata` for
containment predicates. Use targeted expression indexes only for special cases
when equality/sort on a single extracted key outperforms GIN.

## Consequences

Positive:
- Faster containment queries than B-tree expression patterns in our dominant
  access path.
- Lower index footprint than broad `jsonb_ops` for containment-heavy workloads.
- Better analyst filter responsiveness without schema expansion.

Negative:
- `jsonb_path_ops` is specialized; broader operator coverage may need
  additional indexes.
- Extra write cost exists for maintaining GIN structures.
- Index strategy must be reviewed if metadata query patterns diversify.