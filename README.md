# Plant Tracker

A Spring Boot app for tracking houseplants, their watering history, and photos, with a browser-based frontend.

## Tech stack

- Java 25, Spring Boot 3.5
- PostgreSQL
- Spring Data JPA
- Maven

## Features

- Add and delete plants
- Upload multiple photos per plant with optional notes
- Photo gallery with thumbnails, lightbox, and delete per image
- Record watering events with amount and notes; weather data (temperature, humidity, weather code) is fetched automatically via Open-Meteo
- Per-plant history with watering chart, temperature chart, and photo slideshow
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
| `DELETE` | `/plants/{id}` | Delete a plant |

`POST /plants` body:
```json
{
  "name": "Monstera",
  "species": "Monstera deliciosa",
  "location": "Living room"
}
```
`name` is required; `species` and `location` are optional.

### Watering events

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/watering` | List all watering events |
| `GET` | `/watering/plant/{plantId}` | Watering history for a plant |
| `POST` | `/watering` | Record a watering |
| `DELETE` | `/watering/{id}` | Delete a watering event |

`POST /watering` body:
```json
{
  "plantId": 1,
  "amountMl": 200,
  "note": "Soil was dry"
}
```
`plantId` is required; `amountMl` must be positive if provided; `note` is optional.

### Analytics

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/analytics/plant/{plantId}` | Analytics for a plant |

Response:
```json
{
  "totalWaterings": 3,
  "averageDaysBetweenWaterings": 4.5,
  "daysSinceLastWatering": 6
}
```
