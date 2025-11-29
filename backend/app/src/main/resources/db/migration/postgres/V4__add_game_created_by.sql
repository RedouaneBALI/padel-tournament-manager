-- Flyway V4: add created_by column to game for standalone game ownership

ALTER TABLE game
  ADD COLUMN IF NOT EXISTS created_by VARCHAR(191);

CREATE INDEX IF NOT EXISTS idx_game_created_by ON game(created_by);

-- Add tie-break points to score table
ALTER TABLE score
  ADD COLUMN IF NOT EXISTS tie_break_point_a INTEGER,
  ADD COLUMN IF NOT EXISTS tie_break_point_b INTEGER;
