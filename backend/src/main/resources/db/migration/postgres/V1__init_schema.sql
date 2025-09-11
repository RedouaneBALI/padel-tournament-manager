-- Flyway V1 — Application schema for PostgreSQL (production)

CREATE TABLE IF NOT EXISTS match_format (
    id BIGSERIAL PRIMARY KEY,
    advantage BOOLEAN NOT NULL DEFAULT true,
    number_of_sets_to_win INTEGER NOT NULL DEFAULT 2 CHECK (number_of_sets_to_win >= 1),
    games_per_set INTEGER NOT NULL DEFAULT 6 CHECK (games_per_set >= 1),
    super_tie_break_in_final_set BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS player (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    birth_year INTEGER,
    points INTEGER,
    ranking INTEGER
);

CREATE TABLE IF NOT EXISTS score (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS set_score (
    id BIGSERIAL PRIMARY KEY,
    order_index INTEGER,
    team_a_score INTEGER NOT NULL,
    team_b_score INTEGER NOT NULL,
    tie_break_team_a INTEGER,
    tie_break_team_b INTEGER,
    score_id BIGINT REFERENCES score(id)
);

CREATE INDEX IF NOT EXISTS idx_set_score_score_id ON set_score(score_id);

CREATE TABLE IF NOT EXISTS tournament (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(191) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    city VARCHAR(50),
    club VARCHAR(50),
    gender VARCHAR(16),
    level VARCHAR(32),
    start_date DATE,
    end_date DATE,
    config JSONB,
    CONSTRAINT tournament_config_is_object CHECK (config IS NULL OR jsonb_typeof(config) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_tournament_owner ON tournament(owner_id);
CREATE INDEX IF NOT EXISTS idx_tournament_config_gin ON tournament USING GIN (config);

CREATE TABLE IF NOT EXISTS round (
    id BIGSERIAL PRIMARY KEY,
    order_index INTEGER,
    match_format_id BIGINT REFERENCES match_format(id),
    tournament_id BIGINT REFERENCES tournament(id),
    stage VARCHAR(255) CHECK (
      (stage)::text = ANY ((ARRAY['GROUPS'::varchar, 'Q1'::varchar, 'Q2'::varchar, 'Q3'::varchar, 'R64'::varchar, 'R32'::varchar, 'R16'::varchar, 'QUARTERS'::varchar, 'SEMIS'::varchar, 'FINAL'::varchar, 'WINNER'::varchar])::text[])
    )
);

CREATE INDEX IF NOT EXISTS idx_round_tournament_id ON round(tournament_id);

CREATE TABLE IF NOT EXISTS pool_ranking (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS pool (
    id BIGSERIAL PRIMARY KEY,
    pool_ranking_id BIGINT REFERENCES pool_ranking(id),
    round_id BIGINT REFERENCES round(id),
    name VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_pool_round_id ON pool(round_id);

CREATE TABLE IF NOT EXISTS player_pair (
    id BIGSERIAL PRIMARY KEY,
    order_index INTEGER,
    seed INTEGER NOT NULL DEFAULT 0,
    type VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    player1_id BIGINT REFERENCES player(id),
    player2_id BIGINT REFERENCES player(id),
    pool_id BIGINT REFERENCES pool(id) ON DELETE SET NULL,
    tournament_id BIGINT REFERENCES tournament(id)
);

CREATE INDEX IF NOT EXISTS idx_player_pair_tournament_id ON player_pair(tournament_id);
CREATE INDEX IF NOT EXISTS idx_player_pair_pool_id ON player_pair(pool_id);

CREATE TABLE IF NOT EXISTS pool_ranking_details (
    id BIGSERIAL PRIMARY KEY,
    order_index INTEGER,
    points INTEGER NOT NULL DEFAULT 0,
    set_average INTEGER NOT NULL DEFAULT 0,
    player_pair_id BIGINT REFERENCES player_pair(id),
    pool_ranking_id BIGINT REFERENCES pool_ranking(id)
);

CREATE INDEX IF NOT EXISTS idx_pool_ranking_details_pair_id ON pool_ranking_details(player_pair_id);
CREATE INDEX IF NOT EXISTS idx_pool_ranking_details_ranking_id ON pool_ranking_details(pool_ranking_id);

CREATE TABLE IF NOT EXISTS game (
    id BIGSERIAL PRIMARY KEY,
    order_index INTEGER,
    scheduled_time TIME(6) WITHOUT TIME ZONE,
    winner_side VARCHAR(16),
    format_id BIGINT REFERENCES match_format(id),
    pool_id BIGINT REFERENCES pool(id) ON DELETE SET NULL,
    round_id BIGINT REFERENCES round(id) ON DELETE CASCADE,
    score_id BIGINT REFERENCES score(id),
    teama_id BIGINT REFERENCES player_pair(id) ON DELETE SET NULL,
    teamb_id BIGINT REFERENCES player_pair(id) ON DELETE SET NULL,
    court VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_game_round_id ON game(round_id);
CREATE INDEX IF NOT EXISTS idx_game_pool_id ON game(pool_id);
CREATE INDEX IF NOT EXISTS idx_game_teama_id ON game(teama_id);
CREATE INDEX IF NOT EXISTS idx_game_teamb_id ON game(teamb_id);
CREATE INDEX IF NOT EXISTS idx_game_score_id ON game(score_id);

-- End V1

