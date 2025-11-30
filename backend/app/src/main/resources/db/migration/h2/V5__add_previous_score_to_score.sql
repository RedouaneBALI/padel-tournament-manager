-- Migration pour ajouter la colonne previous_score_id à la table score (H2)
ALTER TABLE score ADD COLUMN IF NOT EXISTS previous_score_id BIGINT NULL;

-- Supprime tous les indexes uniques possibles sur previous_score_id
DROP INDEX IF EXISTS CONSTRAINT_INDEX_4B;
DROP INDEX IF EXISTS UK_SCORE_PREVIOUS_SCORE_ID;
DROP INDEX IF EXISTS SCORE_PREVIOUS_SCORE_ID_INDEX;

-- Supprime la contrainte de clé étrangère si elle existe déjà
ALTER TABLE score DROP CONSTRAINT IF EXISTS fk_score_previous_score;
ALTER TABLE score DROP CONSTRAINT IF EXISTS uk_score_previous_score_id;

-- Ajoute la contrainte de clé étrangère SANS unique
ALTER TABLE score ADD CONSTRAINT fk_score_previous_score FOREIGN KEY (previous_score_id) REFERENCES score(id);
