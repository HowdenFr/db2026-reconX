# ADR-0001 - Partition the `trades` table by `trade_date`

## Status

Accepted | Date: 2026-07-27

## Context

ReconX ingests about 50,000 trades/day with 5-year retention, which yields
roughly 91 million rows at steady state. Most analyst and reconciliation
queries filter by trade date ranges (day or month). We also need predictable
retention operations and low-latency reads for 10 concurrent recon analysts.

Alternatives considered:
- Keep one unpartitioned `trades` table with larger indexes.
- Partition by `counterparty_id`.
- Partition by `trade_date` month ranges.

Constraints/forces:
- PostgreSQL 16 partitioning behavior and key rules.
- Date-range query pattern dominates operational traffic.
- Retention/archive must be operationally safe and fast.

## Decision

Use RANGE partitioning on `trade_date` with monthly partitions named
`trades_yYYYYmMM`. Pre-create the next 12 months of partitions. Keep a default
partition to prevent write failures for out-of-range dates and alert on any
rows landing there.

## Consequences

Positive:
- Partition pruning cuts scanned data for typical month-filtered queries.
- Retention is managed with partition detach/drop instead of mass deletes.
- Per-partition indexes are smaller and faster to maintain.

Negative:
- Composite-key and partition-key implications increase ORM mapping complexity.
- Partition lifecycle automation becomes an ongoing operations responsibility.
- Cross-partition uniqueness patterns require careful schema design.