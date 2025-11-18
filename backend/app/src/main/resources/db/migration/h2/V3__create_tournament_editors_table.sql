-- H2 migration: create tournament_editors table
CREATE TABLE IF NOT EXISTS tournament_editors (
  tournament_id BIGINT NOT NULL,
  editor_id VARCHAR(191) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tournament_editors_editor_id ON tournament_editors(editor_id);

