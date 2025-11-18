-- H2 migration: add qualifier_index column to player_pair
ALTER TABLE player_pair ADD COLUMN qualifier_index INTEGER;

