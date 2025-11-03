-- Add qualifier_index column to player_pair table
-- This column stores the qualifier number (1 for Q1, 2 for Q2, etc.)
-- Only used when type = 'QUALIFIER', null for all other types

ALTER TABLE player_pair
ADD COLUMN qualifier_index INTEGER;

-- Add a comment to document the column purpose
COMMENT ON COLUMN player_pair.qualifier_index IS 'Index of the qualifier (1 for Q1, 2 for Q2, etc.). Only used when type = QUALIFIER, null otherwise.';

