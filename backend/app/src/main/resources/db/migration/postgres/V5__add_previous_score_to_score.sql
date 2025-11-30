-- Migration pour ajouter la colonne previous_score_id à la table score (PostgreSQL)
ALTER TABLE score ADD COLUMN previous_score_id BIGINT NULL;

-- Supprime la contrainte si elle existe déjà (pour éviter les doublons)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_score_previous_score') THEN
    ALTER TABLE score DROP CONSTRAINT fk_score_previous_score;
  END IF;
END $$;

-- Ajoute la contrainte de clé étrangère SANS unique
ALTER TABLE score ADD CONSTRAINT fk_score_previous_score FOREIGN KEY (previous_score_id) REFERENCES score(id);

