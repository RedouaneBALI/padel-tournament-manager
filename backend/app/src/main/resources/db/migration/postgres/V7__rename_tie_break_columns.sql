-- Flyway V7: align tie-break column naming in score table
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name='score' AND column_name='tie_break_pointa'
  ) THEN
    EXECUTE 'ALTER TABLE score RENAME COLUMN tie_break_pointa TO tie_break_point_a';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name='score' AND column_name='tie_break_pointb'
  ) THEN
    EXECUTE 'ALTER TABLE score RENAME COLUMN tie_break_pointb TO tie_break_point_b';
  END IF;
END $$;

