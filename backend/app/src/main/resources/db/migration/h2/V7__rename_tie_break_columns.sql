-- Flyway V7: align tie-break column naming in score table
-- Note: H2 does not support IF EXISTS for RENAME, and columns are created in V8
-- This migration is a no-op on H2 (columns created with correct names in V8)
-- Postgres version handles conditional rename via PL/pgSQL
SELECT 1;


