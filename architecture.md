# Architecture: Plant Tracker

A Spring Boot REST API with a vanilla JS frontend for tracking houseplants, the rooms they
live in, their watering history and their photos.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5.0 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Validation | spring-boot-starter-validation |
| Build | Maven |
| Frontend | Vanilla HTML/JS served as static resources |
| Charting | Chart.js 4.4.9 + chartjs-adapter-date-fns |
| Maps | Leaflet 1.9.4 (weather location picker) |
| QR codes | ZXing |
| External APIs | Open-Meteo (weather), Nominatim (geocoding) |

## Package Structure

```
org.example.plants
├── PlantsApplication.java           # Entry point (@SpringBootApplication)
├── controller/
│   ├── PlantController.java         # /plants — CRUD, images, QR code, room assignment
│   ├── WateringController.java      # /watering — single and whole-room watering
│   ├── AnalyticsController.java     # /analytics — per-plant watering frequency
│   ├── RoomController.java          # /rooms — CRUD plus per-room statistics
│   └── SettingsController.java      # /settings — persisted weather location
├── service/
│   ├── PlantService.java            # Plant CRUD, image storage on disk
│   ├── WateringService.java         # Watering events, weather enrichment
│   ├── AnalyticsService.java        # Average and median interval, days since last
│   ├── RoomService.java             # Room CRUD, per-room statistics
│   └── WeatherService.java          # Open-Meteo lookups, location persistence
├── repository/                      # One JpaRepository per entity
├── model/
│   ├── Plant.java                   # id, name, species, location, room (@ManyToOne), createdAt
│   ├── Room.java                    # id, name (unique)
│   ├── PlantImage.java              # id, plantId, imagePath, note, createdAt
│   ├── WateringEvent.java           # id, plant (@ManyToOne), wateredAt, amountMl, note,
│   │                                #   fertilizer, weather snapshot
│   └── AppSettings.java             # single row (id = 1): latitude, longitude
└── dto/
    ├── PlantCreateRequest.java      # name, species, location, roomId
    ├── WaterCreateRequest.java      # nested WateringRequest(plantId, amountMl, note, fertilizer)
    ├── BulkWateringRequest.java     # plantIds, note, fertilizer — no amount, see below
    ├── PlantAnalytics.java          # totalWaterings, average/median interval, daysSinceLast
    ├── RoomCreateRequest.java       # name
    ├── RoomStats.java               # roomId, name, plantCount, daysSinceLast, average, overdue
    └── OverduePlant.java            # plantId, name, daysSinceLast, medianDaysBetweenWaterings
```

## Frontend

Static files served from `src/main/resources/static/`:

- **`index.html`** — four-tab layout plus a lightbox for photos
- **`app.js`** — all API calls via `fetch()`; no build tooling or framework
- **`water.html`** — standalone quick-watering page, opened by scanning a plant's QR code

| Tab | Features |
|---|---|
| Plants | Add a plant (name, species, room, spot), photo upload and gallery with lightbox, QR code, delete |
| Watering | Water a whole room via checkboxes, record a single watering, weather location picker, event list |
| Analytics | Per plant: room and spot editing, photo slideshow, stat cards, watering and temperature charts, history |
| Rooms | Per room: plant count, last watered, average interval, overdue plants; add, rename, delete |

The selected plant on the Watering and Analytics tabs is kept per tab in `localStorage`, so
switching tabs or reloading does not reset the dropdown.

## Database Schema

```sql
rooms (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL UNIQUE
)

plants (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  species     VARCHAR(100),
  location    VARCHAR(100),                -- spot within the room, e.g. "windowsill"
  room_id     BIGINT NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
)

plant_images (
  id          BIGSERIAL PRIMARY KEY,
  plant_id    BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
  image_path  VARCHAR(255) NOT NULL,       -- file on disk, under app.upload-dir
  note        TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
)

watering_events (
  id                BIGSERIAL PRIMARY KEY,
  plant_id          BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
  watered_at        TIMESTAMP NOT NULL DEFAULT NOW(),
  amount_ml         INTEGER,
  note              TEXT,
  fertilizer        BOOLEAN NOT NULL DEFAULT FALSE,
  temperature_c     REAL,                  -- weather snapshot at the time of watering
  min_temperature_c REAL,
  max_temperature_c REAL,
  humidity_percent  REAL,
  weather_code      INTEGER
)

app_settings (
  id          INTEGER PRIMARY KEY,         -- always 1; a single row of app configuration
  latitude    DOUBLE PRECISION NOT NULL,
  longitude   DOUBLE PRECISION NOT NULL
)

INDEX idx_plants_room_id             ON plants(room_id)
INDEX idx_plant_images_plant_id      ON plant_images(plant_id)
INDEX idx_watering_events_plant_id   ON watering_events(plant_id)
INDEX idx_watering_events_watered_at ON watering_events(watered_at)
```

- Schema comes entirely from `schema.sql`; Hibernate runs with `ddl-auto=none`
- `schema.sql` runs on **every** startup (`spring.sql.init.mode=always`), so every statement
  must be idempotent — hence `IF NOT EXISTS` throughout
- `data.sql` holds commented-out development seed data; uncomment it to populate a fresh
  database, and note each block is guarded so it cannot insert twice
- Credentials come from the `DB_USERNAME` and `DB_PASSWORD` environment variables

## REST API

### Plants — `/plants`

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/plants` | List all plants | `200` + plant array |
| `POST` | `/plants` | Create a plant | `201` + plant, `400` if the room is missing or unknown |
| `PATCH` | `/plants/{id}` | Update the spot within the room | `200` + plant, `404` |
| `PUT` | `/plants/{id}/room` | Move the plant to another room | `200` + plant, `400` if null, `404` |
| `DELETE` | `/plants/{id}` | Delete a plant | `204` |
| `GET` | `/plants/{id}/qr` | PNG QR code linking to the quick-watering page | `200` + image |
| `POST` | `/plants/{id}/images` | Upload a photo (multipart `file`, optional `note`) | `201` + image |
| `GET` | `/plants/{id}/images` | List a plant's photos, oldest first | `200` + image array |
| `GET` | `/plants/{id}/images/{imageId}` | Photo bytes, content type from the file extension | `200` + image, `404` |
| `DELETE` | `/plants/{id}/images/{imageId}` | Delete a photo and its file | `204`, `404` |

```json
POST /plants           { "name": "Monstera", "species": "Monstera deliciosa",
                         "location": "windowsill", "roomId": 1 }
PATCH /plants/{id}     { "location": "north shelf" }     // blank clears it
PUT /plants/{id}/room  { "roomId": 3 }                   // null is rejected
```

### Rooms — `/rooms`

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/rooms` | List rooms, by name | `200` + room array |
| `GET` | `/rooms/stats` | Per-room statistics | `200` + stats array |
| `GET` | `/rooms/{id}/plants` | Plants in a room | `200` + plant array |
| `POST` | `/rooms` | Create a room | `201` + room, `400` blank, `409` duplicate |
| `PATCH` | `/rooms/{id}` | Rename a room | `200` + room, `404`, `409` duplicate |
| `DELETE` | `/rooms/{id}` | Delete an empty room | `204`, `404`, `409` if it still has plants |

```json
GET /rooms/stats
[ { "roomId": 1, "name": "Living room", "plantCount": 3,
    "daysSinceLastWatering": 2, "averageDaysBetweenWaterings": 6.0,
    "overduePlants": [ { "plantId": 11, "name": "Pothos",
                         "daysSinceLastWatering": 20,
                         "medianDaysBetweenWaterings": 5.0 } ] } ]
```

### Watering — `/watering`

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/watering` | Record one watering | `201` + event, `400` |
| `POST` | `/watering/bulk` | Record one event per plant, for a whole room | `201` + event array, `400` |
| `GET` | `/watering` | List all events | `200` + event array |
| `GET` | `/watering/plant/{plantId}` | A plant's history, newest first | `200` + event array |
| `DELETE` | `/watering/{id}` | Delete an event | `204` |

```json
POST /watering        { "plantId": 1, "amountMl": 200, "note": "looked dry",
                        "fertilizer": true }
POST /watering/bulk   { "plantIds": [1, 2, 5], "note": "weekly round",
                        "fertilizer": false }
```

Every watering is stamped with the current weather from Open-Meteo. A bulk request fails
as a whole if any id is unknown or repeated, rather than watering a subset.

### Analytics — `/analytics`

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/analytics/plant/{plantId}` | Watering frequency for a plant | `200` + analytics JSON |

```json
{ "totalWaterings": 5, "averageDaysBetweenWaterings": 11.25,
  "medianDaysBetweenWaterings": 5.0, "daysSinceLastWatering": 2 }
```

Both intervals are `null` until a plant has two waterings. Gaps are measured in calendar
days, so the time of day cannot shift them.

### Settings — `/settings`

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/settings/location` | Weather location plus whether it is saved | `200` |
| `POST` | `/settings/location` | Update and persist the weather location | `200` |

Persisted in `app_settings`, falling back to the `LATITUDE` / `LONGITUDE` environment
variables when no row exists yet.

## Notable Design Decisions

- **A plant always belongs to a room.** `room_id` is `NOT NULL`, so `roomId` is required
  when creating a plant and cannot be set to null afterwards. The foreign key is
  `ON DELETE RESTRICT` and deleting a room that still holds plants returns `409`, rather
  than leaving plants homeless. `location` narrowed in meaning to the spot within the room.
- **Lazy associations expose only ids.** `Plant.getRoom()` and `WateringEvent.getPlant()`
  are `@JsonIgnore`, with `getRoomId()` / `getPlantId()` serialized instead.
  `spring.jpa.open-in-view=false` means there is no Hibernate session during serialization,
  so reading a lazy association's *name* would throw `LazyInitializationException`. The
  frontend resolves names from its own cached `/rooms` and `/plants` lists.
- **Median decides whether a plant is due.** Forgetting one plant for a month drags its
  average up, so a five day rhythm with one thirty day lapse averages eleven and looks
  healthy. `RoomStats` therefore flags overdue plants against the median, while still
  reporting the average as a description of overall history.
- **One weather lookup per watering round.** `WateringService` fetches weather once for a
  bulk request and shares it across every event, so a room of six makes one Open-Meteo call
  and all its events carry identical readings.
- **No amount on a bulk watering.** A round rarely gives every plant the same volume, so
  amounts are only recorded on single events.
- **Images on disk, metadata in the database.** Files live under `app.upload-dir` with a
  UUID filename; `plant_images` stores the path. Content type is derived from the extension
  via `MediaTypeFactory`.
- **Weather location loads on `ApplicationReadyEvent`**, not `@PostConstruct`, because the
  latter runs before `schema.sql` has created `app_settings`.
- **DTOs for write operations** keep the API contract separate from the entities, so clients
  cannot set internal fields like `id` or `createdAt`. Jakarta Validation annotations
  (`@NotBlank`, `@NotNull`, `@NotEmpty`, `@Positive`) are activated by `@Valid`.
- **Static files are served from the source folder in development**
  (`spring.web.resources.static-locations`) with caching disabled, so HTML/JS edits need no
  rebuild. The `classpath:/static/` fallback is what the packaged jar and Docker image use.

## Tests

82 tests, in two layers:

| Layer | Annotation | Covers |
|---|---|---|
| Unit | `@ExtendWith(MockitoExtension.class)` | Service logic: intervals, overdue boundaries, room rules, file handling |
| Web | `@WebMvcTest` + `@MockitoBean` | Status codes, validation, JSON shape |

There are no integration tests against a real database, and no frontend tests — so the
migration behaviour in `schema.sql` and everything in `app.js` is only verified by hand.
Run with `mvn verify`.
