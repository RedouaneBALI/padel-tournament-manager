-- Migration Flyway : Ajout des colonnes pour le score du jeu en direct
ALTER TABLE score
ADD COLUMN current_game_point_a VARCHAR(20) DEFAULT 'ZERO',
ADD COLUMN current_game_point_b VARCHAR(20) DEFAULT 'ZERO';

