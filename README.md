# Plant Tracker

A Spring Boot REST API for tracking houseplants and their watering history, with a small analytics layer and a browser-based frontend.

## Tech stack

- Java 25, Spring Boot 3.5
- PostgreSQL
- Spring Data JPA
- Maven

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

The schema is created automatically on first start. Sample data (two plants + watering events) is loaded on every startup via `data.sql`.

The frontend is available at `http://localhost:8080`.

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
