# Architecture: Plant Tracker

A Spring Boot REST API with a vanilla JS frontend for tracking houseplants and their watering history.

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

## Package Structure

```
org.example.plants
├── PlantsApplication.java           # Entry point (@SpringBootApplication)
├── controller/
│   ├── PlantController.java         # REST endpoints: /plants
│   ├── WateringController.java      # REST endpoints: /watering
│   └── AnalyticsController.java     # REST endpoints: /analytics
├── service/
│   ├── PlantService.java            # Plant CRUD logic
│   ├── WateringService.java         # Watering event logic
│   └── AnalyticsService.java        # Watering frequency analytics
├── repository/
│   ├── PlantRepository.java         # JpaRepository<Plant, Long>
│   └── WateringEventRepository.java # JpaRepository + findByPlant_IdOrderByWateredAtDesc
├── model/
│   ├── Plant.java                   # @Entity: id, name, species, location, createdAt
│   └── WateringEvent.java           # @Entity: id, plant (@ManyToOne), wateredAt, amountMl, note
└── dto/
    ├── PlantCreateRequest.java      # record: name, species, location
    ├── WaterCreateRequest.java      # nested record: WateringRequest(plantId, amountMl, note)
    └── PlantAnalytics.java          # record: totalWaterings, averageDaysBetweenWaterings, daysSinceLastWatering
```

## Frontend

Static files served by Spring Boot from `src/main/resources/static/`:

- **`index.html`** — three-tab layout: Plants, Watering, History
- **`app.js`** — all API calls via `fetch()`; no build tooling or framework

| Tab | Features |
|---|---|
| Plants | Add a plant (name, species, location), list all plants, delete |
| Watering | Record a watering event (plant dropdown, amount, note), list all events, delete |
| History | Select a plant → analytics stat cards + time-axis line chart of watering events |

## Database Schema

```sql
plants (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  species     VARCHAR(100),
  location    VARCHAR(100),
  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
)

watering_events (
  id          BIGSERIAL PRIMARY KEY,
  plant_id    BIGINT NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
  watered_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  amount_ml   INTEGER,
  note        TEXT
)

INDEX idx_watering_events_plant_id   ON watering_events(plant_id)
INDEX idx_watering_events_watered_at ON watering_events(watered_at)
```

- Schema is created from `schema.sql`; Hibernate is set to `validate` (not auto-create)
- Seed data in `data.sql` inserts 2 plants and 3 watering events on startup
- Credentials are supplied via environment variables `DB_USERNAME` and `DB_PASSWORD`

## REST API

### Plants — `/plants`

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/plants` | Create a plant | `201 Created` + plant JSON |
| `GET` | `/plants` | List all plants | `200 OK` + plant array |
| `DELETE` | `/plants/{id}` | Delete a plant | `204 No Content` |

**POST /plants request body:**
```json
{ "name": "Monstera", "species": "Monstera deliciosa", "location": "Living room" }
```

### Watering — `/watering`

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/watering` | Record a watering event | `201 Created` + event JSON |
| `GET` | `/watering` | List all watering events | `200 OK` + event array |
| `GET` | `/watering/plant/{plantId}` | Watering history for a plant (newest first) | `200 OK` + event array |
| `DELETE` | `/watering/{id}` | Delete a watering event | `204 No Content` |

**POST /watering request body:**
```json
{ "plantId": 1, "amountMl": 200, "note": "looked dry" }
```

### Analytics — `/analytics`

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/analytics/plant/{plantId}` | Frequency analytics for a plant | `200 OK` + analytics JSON |

**GET /analytics/plant/{plantId} response:**
```json
{ "totalWaterings": 5, "averageDaysBetweenWaterings": 3.5, "daysSinceLastWatering": 2 }
```
`averageDaysBetweenWaterings` is `null` if fewer than 2 events exist.

## Notable Design Decisions

- **`@ManyToOne` association**: `WateringEvent` holds a lazy-loaded reference to `Plant` via `@ManyToOne(fetch = FetchType.LAZY)`. The JSON response still exposes `plantId` via a convenience getter to keep the API contract stable. Be aware of the N+1 risk if navigating the association in a loop — use `JOIN FETCH` if that arises.
- **DTOs for write operations**: `PlantCreateRequest` and `WaterCreateRequest` keep the API contract separate from the entity model, preventing clients from setting internal fields like `id` or `createdAt`.
- **Validation**: Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Positive`) on DTOs, activated by `@Valid` on controller parameters.
- **Logging**: SLF4J via `LoggerFactory` — `log.info` for successful deletes, `log.warn` for not-found deletes.
