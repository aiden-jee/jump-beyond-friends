ALTER TABLE players
    DROP CONSTRAINT IF EXISTS uq_players_name;

ALTER TABLE players
    ADD COLUMN google_sub VARCHAR(255),
    ADD COLUMN avatar_url TEXT,
    ADD COLUMN verified_at TIMESTAMPTZ;

ALTER TABLE players
    ADD CONSTRAINT uq_player_google_sub UNIQUE (google_sub);