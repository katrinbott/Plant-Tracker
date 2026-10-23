-- Test data for the overdue display.
--
-- Run this by hand when you want it; nothing runs it for you. See seed/README.md.
--
-- Every name says what it should show, so the Analytics dropdown doubles as a checklist.
-- Delete the three Test rooms and their plants to remove it all.
INSERT INTO rooms (name)
SELECT seed.name
FROM (VALUES ('Test room'), ('Test windowsill'), ('Test attic')) AS seed(name)
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE rooms.name = seed.name);

-- Test attic deliberately stays empty: it shows "no plants yet" on the Rooms tab and is
-- the only one of the three that can be deleted without moving plants first.
INSERT INTO plants (name, species, location, room_id)
SELECT seed.name, seed.species, seed.location, rooms.id
FROM (VALUES
    ('Overdue pothos',      'Epipremnum aureum',      'shelf',      'Test room'),
    ('On-rhythm monstera',  'Monstera deliciosa',     'windowsill', 'Test room'),
    ('Exactly-due basil',   'Ocimum basilicum',       'counter',    'Test room'),
    ('Same-day lavender',   'Lavandula angustifolia', 'sill',       'Test windowsill'),
    ('Once-watered fern',   'Nephrolepis exaltata',   'sill',       'Test windowsill'),
    ('Never-watered cactus','Echinopsis chamaecereus','sill',       'Test windowsill')
) AS seed(name, species, location, room)
JOIN rooms ON rooms.name = seed.room
WHERE NOT EXISTS (SELECT 1 FROM plants WHERE plants.name = seed.name);

-- What each plant should show on the Analytics tab:
--   Overdue pothos      gaps 5,5,5,30 → median 5.0, mean 11.25, watered 8 days ago.
--                       Days since last watering is RED. The mean would not flag it,
--                       since 8 < 11.25 — this is the case the median exists for.
--   On-rhythm monstera  gaps 7,7,7 → median 7.0, watered 2 days ago. Nothing red.
--   Exactly-due basil   gaps 5,5 → median 5.0, watered 5 days ago. Nothing red: the
--                       rule is strictly greater than, so exactly on the median is fine.
--   Same-day lavender   two waterings on one date → no average, no median, 2 waterings.
--   Once-watered fern   one watering → no average, no median.
--   Never-watered cactus  no waterings → 0 and three dashes.
--
-- Dates are built from CURRENT_DATE so whole-day gaps are exact regardless of the hour
-- the database is seeded, which matters for the same-day pair.
INSERT INTO watering_events (
    plant_id, watered_at, amount_ml, note, fertilizer,
    temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
SELECT plants.id, (CURRENT_DATE - seed.days_ago) + seed.at_time, seed.amount_ml, seed.note,
       seed.fertilizer, seed.temperature_c, seed.min_temperature_c, seed.max_temperature_c,
       seed.humidity_percent, seed.weather_code
FROM (VALUES
    ('Overdue pothos',     53, TIME '09:00', 150, 'Fed along with water', true,  19.5, 14.2, 23.8, 58.0, 0),
    ('Overdue pothos',     23, TIME '09:00', 150, NULL,                   false, 17.8, 12.9, 21.5, 64.0, 2),
    ('Overdue pothos',     18, TIME '09:00', 120, 'Leaves a bit pale',    false, 16.2, 11.4, 20.1, 69.0, 3),
    ('Overdue pothos',     13, TIME '09:00', 150, NULL,                   false, 15.0, 10.2, 18.9, 73.0, 1),
    ('Overdue pothos',      8, TIME '09:00', 150, NULL,                   false, 14.4,  9.7, 18.0, 70.0, 2),

    ('On-rhythm monstera', 23, TIME '09:00', 200, 'Weekly watering',      true,  18.2, 13.1, 22.4, 61.0, 1),
    ('On-rhythm monstera', 16, TIME '09:00', 200, NULL,                   false, 16.9, 12.0, 20.8, 68.0, 3),
    ('On-rhythm monstera',  9, TIME '09:00', 180, 'Soil quite dry',       false, 15.4, 10.6, 19.2, 72.0, 61),
    ('On-rhythm monstera',  2, TIME '09:00', 220, 'New growth appearing', true,  12.8,  8.4, 16.1, 70.0, 0),

    ('Exactly-due basil',  15, TIME '09:00',  80, 'Picked some leaves',   false, 14.6,  9.9, 18.2, 71.0, 2),
    ('Exactly-due basil',  10, TIME '09:00',  80, NULL,                   false, 13.9,  9.2, 17.4, 72.0, 1),
    ('Exactly-due basil',   5, TIME '09:00',  80, NULL,                   false, 13.2,  8.8, 16.9, 74.0, 61),

    -- One date, two events: the gap between them is not a watering interval.
    ('Same-day lavender',   3, TIME '09:00', 120, 'First watering outside', false, 12.4, 7.6, 15.8, 66.0, 1),
    ('Same-day lavender',   3, TIME '18:00',  60, 'Still dry, topped up',   false, 14.1, 7.6, 15.8, 59.0, 1),

    ('Once-watered fern',   4, TIME '09:00', 100, NULL,                   false, 13.5,  9.0, 17.1, 75.0, 3)
) AS seed(plant, days_ago, at_time, amount_ml, note, fertilizer,
          temperature_c, min_temperature_c, max_temperature_c, humidity_percent, weather_code)
JOIN plants ON plants.name = seed.plant
WHERE NOT EXISTS (SELECT 1 FROM watering_events w WHERE w.plant_id = plants.id);
