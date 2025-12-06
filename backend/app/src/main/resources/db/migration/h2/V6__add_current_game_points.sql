-- Flyway V6: ensure current game point columns exist on score (prod hotfix)
ALTER TABLE score ADD COLUMN IF NOT EXISTS current_game_point_a VARCHAR(20);
ALTER TABLE score ADD COLUMN IF NOT EXISTS current_game_point_b VARCHAR(20);

-- backfill default values to keep enum happy
UPDATE score
SET current_game_point_a = COALESCE(current_game_point_a, 'ZERO'),
    current_game_point_b = COALESCE(current_game_point_b, 'ZERO');

