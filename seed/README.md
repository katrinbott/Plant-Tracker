# Seed data

SQL you run by hand when you want rows in your local database. Nothing here runs
automatically — that is the point. There is deliberately no `src/main/resources/data.sql`,
because Spring runs that on every startup, which is wrong for a database you keep.

| File | What it adds |
|---|---|
| `demo-data.sql` | Six plants across Living room, Kitchen and Balcony, with watering histories that exercise the analytics: a steady rhythm, one overdue, one with two events, one with a single event, two never watered. |
| `overdue-test-data.sql` | Three `Test *` rooms and six plants named after what they should show, for checking the overdue display. The file lists the expected value for each. |

Every statement is guarded by name, so a file only adds what is missing and running it twice
changes nothing. You can safely run either against a database that already has your own plants.

## Running one

**From IntelliJ** — open the `.sql` file, pick your `plant_tracker` data source in the toolbar,
and execute. The connection is already configured, so there are no credentials to pass.

**With psql**, against the local PostgreSQL the app uses by default
(`application.properties` points at `localhost:5432`):

```
psql -h localhost -U "$DB_USERNAME" -d plant_tracker -f seed/demo-data.sql
```

**Against the Docker database**, if you are running the full stack with `docker compose`. The
`db` service publishes no port, so connect inside the container and pipe the file in:

```
docker compose exec -T db psql -U "$DB_USERNAME" -d plant_tracker < seed/demo-data.sql
```

## Removing it again

`demo-data.sql`'s plants are named after real houseplants, so delete them individually.
`overdue-test-data.sql` puts everything in rooms prefixed `Test `, so emptying and deleting
those three rooms removes all of it.
