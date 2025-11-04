-- Create table to store additional editors for a tournament
CREATE TABLE IF NOT EXISTS tournament_editors (
  tournament_id bigint NOT NULL,
  editor_id varchar(191) NOT NULL
);

-- Add foreign key if tournament table exists (not enforced in initial migration if cross-module)
-- ALTER TABLE tournament_editors
-- ADD CONSTRAINT fk_tournament_editors_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id) ON DELETE CASCADE;

-- Index to allow quick lookup
CREATE INDEX IF NOT EXISTS idx_tournament_editors_editor_id ON tournament_editors(editor_id);

