CREATE TABLE IF NOT EXISTS plants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(100),
    location VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS plant_images (
    id BIGSERIAL PRIMARY KEY,
    plant_id BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    image_path VARCHAR(255) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_plant_images_plant_id ON plant_images(plant_id);

CREATE TABLE IF NOT EXISTS watering_events (
    id BIGSERIAL PRIMARY KEY,
    plant_id BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    watered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    amount_ml INTEGER,
    note TEXT
    );

CREATE INDEX IF NOT EXISTS idx_watering_events_plant_id ON watering_events(plant_id);
CREATE INDEX IF NOT EXISTS idx_watering_events_watered_at ON watering_events(watered_at);

CREATE TABLE IF NOT EXISTS app_settings (
    id INTEGER PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

ALTER TABLE plant_images ADD COLUMN IF NOT EXISTS note TEXT;

ALTER TABLE watering_events ADD COLUMN IF NOT EXISTS temperature_c REAL;
ALTER TABLE watering_events ADD COLUMN IF NOT EXISTS humidity_percent REAL;
ALTER TABLE watering_events ADD COLUMN IF NOT EXISTS weather_code INTEGER;
ALTER TABLE watering_events ADD COLUMN IF NOT EXISTS min_temperature_c REAL;
ALTER TABLE watering_events ADD COLUMN IF NOT EXISTS max_temperature_c REAL;
