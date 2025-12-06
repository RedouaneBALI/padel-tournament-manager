-- Flyway V7: align tie-break column naming in score table
ALTER TABLE score RENAME COLUMN tie_break_pointa TO tie_break_point_a;
ALTER TABLE score RENAME COLUMN tie_break_pointb TO tie_break_point_b;

