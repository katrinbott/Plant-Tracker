CREATE TABLE IF NOT EXISTS plants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(100),
    location VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS watering_events (
    id BIGSERIAL PRIMARY KEY,
    plant_id BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    watered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    amount_ml INTEGER,
    note TEXT
    );

CREATE INDEX IF NOT EXISTS idx_watering_events_plant_id ON watering_events(plant_id);
CREATE INDEX IF NOT EXISTS idx_watering_events_watered_at ON watering_events(watered_at);