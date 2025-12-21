-- Add organizer_name column to tournament table
ALTER TABLE tournament ADD COLUMN organizer_name VARCHAR(100);

-- Add is_featured column to tournament table
ALTER TABLE tournament ADD COLUMN is_featured BOOLEAN DEFAULT false;
