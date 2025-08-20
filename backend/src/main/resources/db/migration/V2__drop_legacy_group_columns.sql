ALTER TABLE tournament
  DROP COLUMN IF EXISTS nb_pools,
  DROP COLUMN IF EXISTS nb_pairs_per_pool,
  DROP COLUMN IF EXISTS nb_qualified_by_pool;