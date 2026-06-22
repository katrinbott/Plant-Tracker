INSERT INTO plants (name, species, location) VALUES
    ('Monstera (desk)', 'Monstera deliciosa', 'Living room'),
    ('Snake plant', 'Sansevieria', 'Office');


-- Monstera (desk) — watered roughly every 8 days
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-04-05 09:00:00', 150, 'Weekly watering', 12.3, 8.1, 15.2, 65.0, 0 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-04-14 08:30:00', 200, 'Soil was very dry', 14.5, 10.2, 18.1, 70.0, 2 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-04-23 09:15:00', 150, null, 11.2, 8.5, 14.0, 85.0, 61 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-02 08:45:00', 200, 'Warm day, watered extra', 16.8, 12.3, 21.5, 60.0, 1 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-10 09:00:00', 150, null, 14.2, 10.8, 17.5, 75.0, 3 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-19 08:30:00', 200, 'Very hot, leaves drooping', 22.1, 16.4, 26.3, 45.0, 0 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-28 09:00:00', 175, null, 19.5, 14.2, 23.8, 55.0, 2 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-06-06 08:45:00', 200, 'Hot week', 24.3, 18.1, 28.5, 40.0, 0 FROM plants WHERE name = 'Monstera (desk)';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-06-14 09:00:00', 200, 'After rain spell', 17.8, 13.2, 22.1, 80.0, 61 FROM plants WHERE name = 'Monstera (desk)';

-- Snake plant — watered roughly every 12 days
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-04-08 09:00:00', 100, null, 13.1, 9.2, 16.8, 68.0, 2 FROM plants WHERE name = 'Snake plant';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-04-21 08:30:00', 120, 'Looked a bit dry', 15.8, 11.5, 19.2, 58.0, 0 FROM plants WHERE name = 'Snake plant';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-04 09:00:00', 100, null, 17.5, 13.1, 22.0, 62.0, 1 FROM plants WHERE name = 'Snake plant';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-17 08:45:00', 120, null, 21.3, 15.8, 25.7, 48.0, 0 FROM plants WHERE name = 'Snake plant';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-05-30 09:00:00', 100, 'Overcast but dry soil', 18.2, 13.9, 22.5, 72.0, 3 FROM plants WHERE name = 'Snake plant';
INSERT INTO watering_events (plant_id, watered_at, amount_ml, note, temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT id, '2026-06-12 08:30:00', 120, null, 20.5, 15.3, 25.1, 55.0, 2 FROM plants WHERE name = 'Snake plant';
