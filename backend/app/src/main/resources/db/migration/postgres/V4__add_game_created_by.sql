-- Flyway V4: add created_by column to game for standalone game ownership

ALTER TABLE game
  ADD COLUMN IF NOT EXISTS created_by VARCHAR(191);

CREATE INDEX IF NOT EXISTS idx_game_created_by ON game(created_by);

