-- ============================================================================
-- TICKET-ADV009 — Sample JSONB payloads for instruments.metadata
--
-- WHAT:    Populates metadata on a handful of already-seeded instruments
--          (see backend/.../changelog/changes/data/instruments.csv) so the
--          containment/path/array queries below have real data to hit.
-- WHY:     008-seed.xml loads instruments via <loadData>, which does not set
--          metadata — every row ships with the DDL default '{}'::jsonb. The
--          GIN index (idx_instruments_metadata_gin, from 003-jsonb.xml) has
--          nothing to index against until rows like these exist.
-- HOW TO RUN: after `docker compose up -d postgres` and a backend boot (so
--          Liquibase has created the schema + loaded the CSV seed), run this
--          file directly, e.g.:
--            psql -h localhost -p 5432 -U reconx -d reconx_dev -f db/seed_data.sql
-- OBSERVE: On a table this small (~15 rows) Postgres' planner will often
--          still pick a Seq Scan over the GIN index regardless of these
--          UPDATEs — that's expected on tiny tables and isn't a sign the
--          index is broken. The point of this file is realistic query
--          results, not proof of index usage.
-- ============================================================================

UPDATE instruments SET metadata = '{
  "sector": "Technology",
  "exchange": "XETR",
  "issuer": {"name": "SAP SE", "country": "DE", "lei": "529900D6BF99LW9R2E68"},
  "rating": {"sp": "AA-", "moody": "Aa3"},
  "tags": ["DAX40", "ESG-tier-1"]
}'::JSONB WHERE symbol = 'SAP.DE';

UPDATE instruments SET metadata = '{
  "sector": "Banking",
  "exchange": "XETR",
  "issuer": {"name": "Deutsche Bank AG", "country": "DE", "lei": "7LTWFZYICNSX8D621K86"},
  "rating": {"sp": "A-", "moody": "A1"},
  "tags": ["DAX40", "G-SIB"]
}'::JSONB WHERE symbol = 'DBKGn.DE';

UPDATE instruments SET metadata = '{
  "sector": "Banking",
  "exchange": "LSE",
  "issuer": {"name": "HSBC Holdings plc", "country": "GB", "lei": "MP6I5ZYZBEU3UXPYFY54"},
  "rating": {"sp": "A-", "moody": "A2"},
  "tags": ["FTSE100", "G-SIB"]
}'::JSONB WHERE symbol = 'HSBA.L';

UPDATE instruments SET metadata = '{
  "sector": "Technology",
  "exchange": "NASDAQ",
  "issuer": {"name": "Apple Inc", "country": "US", "lei": "HWUPKR0MPOU8FGXBT394"},
  "rating": {"sp": "AAA", "moody": "Aaa"},
  "tags": ["NASDAQ100", "mega-cap"]
}'::JSONB WHERE symbol = 'AAPL';

UPDATE instruments SET metadata = '{
  "sector": "Technology",
  "exchange": "NASDAQ",
  "issuer": {"name": "Microsoft Corp", "country": "US", "lei": "INR2EJN1ERAN0W5ZP974"},
  "rating": {"sp": "AAA", "moody": "Aaa"},
  "tags": ["NASDAQ100", "mega-cap"]
}'::JSONB WHERE symbol = 'MSFT';

UPDATE instruments SET metadata = '{
  "sector": "Industrials",
  "exchange": "XETR",
  "issuer": {"name": "Siemens AG", "country": "DE", "lei": "W38RS0KV5LEHAX6JIH49"},
  "rating": {"sp": "A+", "moody": "A1"},
  "tags": ["DAX40"]
}'::JSONB WHERE symbol = 'SIE.DE';

-- Containment (uses idx_instruments_metadata_gin):
SELECT symbol, metadata->>'sector' AS sector
FROM instruments
WHERE metadata @> '{"sector": "Banking"}';

-- Path extraction:
SELECT symbol, metadata->'issuer'->>'country' AS country FROM instruments;

-- Array membership:
SELECT symbol FROM instruments WHERE metadata->'tags' ? 'DAX40';

-- Existence:
SELECT symbol FROM instruments WHERE metadata ? 'rating';
