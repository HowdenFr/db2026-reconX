# ADR-0002 - Store instrument metadata in `JSONB`

## Status

Accepted | Date: 2026-07-27

## Context

ReconX needs flexible instrument attributes (sector, rating, issuer tags,
market-specific flags) that change more frequently than core relational fields.
At our scale (~50,000 trades/day, ~91M trade rows retained, 10 concurrent
analysts), metadata-driven filters must stay queryable without weekly schema
migrations.

Alternatives considered:
- Fully normalized metadata tables (EAV style).
- Plain `TEXT` JSON payloads.
- PostgreSQL `JSONB` in `instruments.metadata`.

Constraints/forces:
- Keep core relational integrity for canonical instrument fields.
- Support ad-hoc filters used by reconciliation investigations.
- Avoid migration churn for non-core attributes.

## Decision

Use a `JSONB` column `instruments.metadata` for non-canonical, evolving
attributes, while keeping core fields (`symbol`, `asset_class`, `currency`,
`isin`) as typed relational columns. Query metadata via JSONB operators and
index targeted access paths.

## Consequences

Positive:
- Metadata can evolve without frequent DDL changes.
- JSONB operators allow selective querying (`@>`, key existence) for analyst
  workflows.
- Relational core remains strict for joins and constraints.

Negative:
- JSON schema is application-governed, not DB-enforced by default.
- Poorly bounded metadata growth can inflate row size and index costs.
- Team needs clear conventions for metadata keys and value types.