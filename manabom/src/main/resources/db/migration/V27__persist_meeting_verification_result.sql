ALTER TABLE meeting_verification
    ADD COLUMN verified_participant_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN final_latitude DOUBLE PRECISION,
    ADD COLUMN final_longitude DOUBLE PRECISION,
    ADD COLUMN verified_has_male BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verified_has_female BOOLEAN NOT NULL DEFAULT FALSE;
