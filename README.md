# Plant Tracker

A Spring Boot app for tracking houseplants, their watering history, and photos, with a browser-based frontend.

## Tech stack

- Java 25, Spring Boot 3.5
- PostgreSQL
- Spring Data JPA
- Maven

## Features

- Organise plants into rooms, with a free-text spot within the room such as "windowsill"
- Water a whole room in one action, unticking any plant you skipped
- Rooms tab with per-room plant count, last watered, average interval, and which plants are overdue
- Add and delete plants; move a plant to another room from the Analytics tab
- Upload multiple photos per plant with optional notes
- Photo gallery with thumbnails, lightbox, and delete per image
- Record watering events with amount, notes, and whether fertilizer was used; weather data (temperature, humidity, weather code) is fetched automatically via Open-Meteo
- Configurable weather location with city search, interactive map picker, and reverse geocoding — persisted in the database
- Per-plant analytics tab with watering stats, watering chart, temperature chart, photo slideshow, and watering history
- Whether a plant is overdue is judged by its median interval, so one forgotten stretch doesn't hide a thirsty plant
- The selected plant on the Watering and Analytics tabs is remembered separately and survives page reloads (stored in the browser's `localStorage`)
- QR codes per plant — scan to open a quick-watering page on your phone
- Docker Compose setup for local development
- CI pipeline via GitHub Actions; deployment to Azure Container Apps

## Prerequisites

- JDK 25+
- PostgreSQL running locally on port 5432

## Setup

1. Create the database:
   ```sql
   CREATE DATABASE plant_tracker;
   ```

2. Set environment variables:
   ```
   DB_USERNAME=<your-db-user>
   DB_PASSWORD=<your-db-password>
   ```

3. Run:
   ```
   mvn spring-boot:run
   ```

The schema is created automatically on first start.

The frontend is available at `http://localhost:8080`.

## Docker

```bash
cp .env.example .env  # fill in DB_USERNAME and DB_PASSWORD
docker compose up --build
```

## API

### Plants

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/plants` | List all plants |
| `POST` | `/plants` | Create a plant |
| `PATCH` | `/plants/{id}` | Update a plant's spot within its room |
| `PUT` | `/plants/{id}/room` | Move a plant to another room |
| `DELETE` | `/plants/{id}` | Delete a plant |
| `GET` | `/plants/{id}/qr` | PNG QR code linking to that plant's quick-watering page |

`POST /plants` body:
```json
{
  "name": "Monstera",
  "species": "Monstera deliciosa",
  "location": "windowsill",
  "roomId": 1
}
```
`name` and `roomId` are required, since every plant belongs to a room; `species` and
`location` are optional. `location` is the spot within the room. Returns `400` if the
room doesn't exist.

`PATCH /plants/{id}` body:
```json
{
  "location": "north shelf"
}
```
Returns the updated plant, or `404` if the plant doesn't exist. A blank `location` clears it.

`PUT /plants/{id}/room` body:
```json
{
  "roomId": 3
}
```
Returns the updated plant, `400` if `roomId` is null or names a room that doesn't
exist, or `404` if the plant doesn't exist.

### Photos

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/plants/{id}/images` | List a plant's photos, oldest first |
| `GET` | `/plants/{id}/images/{imageId}` | Photo bytes |
| `POST` | `/plants/{id}/images` | Upload a photo |
| `DELETE` | `/plants/{id}/images/{imageId}` | Delete a photo |

`POST /plants/{id}/images` is `multipart/form-data`, not JSON:

| Part | Required | Description |
|------|----------|-------------|
| `file` | yes | The image file |
| `note` | no | A note about the photo, e.g. "chopped back due to pests" |

Returns `201` with the stored image. Files are written under `app.upload-dir` with a
generated UUID filename, and the database row keeps the path, so uploading two photos with
the same filename is fine.

`GET /plants/{id}/images/{imageId}` returns the raw bytes with the content type derived from
the file extension, or `404` if the image or its file is missing. Deleting removes both the
row and the file.

### Rooms

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/rooms` | List all rooms |
| `GET` | `/rooms/stats` | Per-room statistics |
| `GET` | `/rooms/{id}/plants` | Plants in a room |
| `POST` | `/rooms` | Create a room |
| `PATCH` | `/rooms/{id}` | Rename a room |
| `DELETE` | `/rooms/{id}` | Delete an empty room |

`POST /rooms` and `PATCH /rooms/{id}` body:
```json
{
  "name": "Living room"
}
```
Names are unique, ignoring case, so a duplicate returns `409`. Deleting a room that still
has plants also returns `409` — move them to another room first.

`GET /rooms/stats` response:
```json
[
  {
    "roomId": 1,
    "name": "Living room",
    "plantCount": 3,
    "daysSinceLastWatering": 2,
    "averageDaysBetweenWaterings": 6.0,
    "overduePlants": [
      {
        "plantId": 11,
        "name": "Pothos",
        "daysSinceLastWatering": 20,
        "medianDaysBetweenWaterings": 5.0
      }
    ]
  }
]
```
A room counts as watered when its most recently watered plant was. A plant is overdue when
it has gone longer than its own median interval, so a plant with fewer than two waterings
is never flagged.

### Watering events

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/watering` | List all watering events |
| `GET` | `/watering/plant/{plantId}` | Watering history for a plant |
| `POST` | `/watering` | Record a watering |
| `POST` | `/watering/bulk` | Record one watering per plant, for a whole room |
| `DELETE` | `/watering/{id}` | Delete a watering event |

`POST /watering` body:
```json
{
  "plantId": 1,
  "amountMl": 200,
  "note": "Soil was dry",
  "fertilizer": true
}
```
`plantId` is required; `amountMl` must be positive if provided; `note` is optional.
`fertilizer` records whether fertilizer was used and defaults to `false` when omitted.

`POST /watering/bulk` body:
```json
{
  "plantIds": [1, 2, 5],
  "note": "weekly round",
  "fertilizer": false
}
```
Creates one event per plant and returns them all. `plantIds` must not be empty, and an
unknown or repeated id fails the whole request rather than watering a subset. There is no
amount, since a round rarely gives every plant the same volume — use single events for
that. The weather is fetched once and shared by every event in the round.

### Analytics

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/analytics/plant/{plantId}` | Analytics for a plant |

Response:
```json
{
  "totalWaterings": 5,
  "averageDaysBetweenWaterings": 11.25,
  "medianDaysBetweenWaterings": 5.0,
  "daysSinceLastWatering": 6,
  "overdue": true
}
```
Both intervals are `null` until a plant has two waterings on separate days, and gaps are
counted between distinct calendar dates, so watering twice in one day counts once. The
average describes the overall history; `overdue` is `true` when the days since the last
watering exceed the *median*, because one forgotten stretch would otherwise raise the bar —
as in the example above, where the real rhythm is five days. The Analytics tab marks the
median tile red when it is set.

### Settings

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/settings/location` | Get current weather location |
| `POST` | `/settings/location` | Update weather location |

`POST /settings/location` body:
```json
{
  "latitude": 53.5511,
  "longitude": 9.9937
}
```

The location is persisted in the database and used to fetch weather data when recording a watering. Falls back to `LATITUDE`/`LONGITUDE` environment variables (defaulting to Berlin) if not yet saved.
