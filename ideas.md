## Ideas and design notes

Forward-looking ideas, the design thinking behind them, and the reasoning behind decisions
already made. The commit-by-commit review log for `feature/rooms` lives in `review-notes.md`,
which is deliberately not committed.

## New potential features

- Add pests table: track occurrence of pests, handling pests, treatments, etc
- Optional parents for plants: if a plant is a cutting or grown through corms they can refer to their parent plant
- Create events: e.g. for moving a plant from one room to another
- Visually mark waterings with fertilizer in the graphs
- More features for a room, potentially cardinal directions (a room might have multiple though?)
- Add an "Edit" button to the plants on the front page, so everything can be edited in one place
- Substrate type: soil or semi-hydro
- Configurable time span on the analytics charts: month, 3 months, 6 months, year, all time.
  Weeks are too short to be worth offering. Both charts currently fit the data, with a floor
  of three days so same-day waterings do not read as separate days (`timeAxisWindow`); a
  chosen range would set the axis window instead, and the floor would still apply to
  "all time" for a plant with one same-day pair
- Pot size as a plant property, plus a repotting event recording the new size (and the
  substrate, if that idea lands) — so the current size is the last repotting, not a field
  edited in place and forgotten. Overlaps the "create events" idea above: a move and a
  repotting are both dated things that happened to a plant
- Archive plants that died, were trashed, sold or given away, as something separate from
  deleting them — and warn before a real delete, which destroys the watering history too
- Read HHCC Flower Care sensors over Bluetooth (room temperature, light intensity, soil
  moisture, fertilization). Sensors are expensive, so only a few plants will ever have one —
  the data has to be optional per plant, not assumed
  - Readings cannot live on `Plant`: a nullable column per metric would make every plant
    carry four empty fields. A separate `sensor_reading` table keyed by plant, with its own
    timestamp, is the same shape as `watering_event`, so the existing charts largely carry over
  - Bluetooth is the awkward part, not the data. The app is a server; reading a BLE device
    needs something in Bluetooth range polling the sensors — a small collector process (a Pi,
    or the laptop) posting to an endpoint here, rather than the server doing it itself. Decide
    pull vs. receive before any schema work

## Suggested order

**Do the edit form (one place to edit everything) first.** Parent plant, room features and
substrate each add one field to a plant. Without an edit form, every one of them needs its own
endpoint and its own scrap of UI — which is how name and species ended up not editable at all,
location edited on the Analytics tab, and room behind a third endpoint. Build the edit form
once and those three become a dropdown each.

**Then decide the event-log shape,** because pests, moves/repotting and substrate changes all
want a timeline of non-watering things and should share one table rather than growing three.

## Design notes per idea

**Pests table** — the largest of these. Main question: is an occurrence a *point in time* or a
*state with a duration*? Only the latter supports "currently affected". Suggest
`pest_occurrences(plant_id, pest, noticed_at, resolved_at NULL, note)` with treatments as a
free-text note first; split treatments into their own table only if you want to track products
and repeat schedules. Pest type as an enum beats free text (spider mites, thrips, fungus gnats,
scale, mealybugs, aphids) — free text will drift into typos and ruin grouping. Photos already
take notes, so linking a photo to an occurrence is a natural later step.

**Parent plant** — one nullable self-referencing FK, `parent_id REFERENCES plants(id) ON DELETE
SET NULL`. Must be nullable, unlike `room_id`: most plants have no parent. SET NULL rather than
RESTRICT, so deleting a mother plant does not require detaching its cuttings first. If the
parent stays editable after creation, add a cheap cycle check (walk up the chain) — two edits
can otherwise make A the parent of B and B the parent of A. Cheap and satisfying: show
"Cuttings: X, Y" on the parent's card.

**Events (moves, repotting, …)** — fits neatly into `PUT /plants/{id}/room`, which today
overwrites `room_id` with no history: record an event there when the room actually changes.
The fork worth deciding deliberately: one `plant_events` table with a type column, or a table
per event type. Watering already has its own table with its own columns, so a generic events
table will sit alongside it — slightly inconsistent, but migrating watering into a generic
table is a lot of risk for no user-visible gain. Recommend: leave watering alone, add
`plant_events` for everything else, and let it carry pests and substrate changes too.

**Room features / cardinal directions** — the original doubt is the right instinct: a room can
face several directions, so direction does not belong on the room. What actually determines
light is the *spot*, which today is free text on the plant. Two options: add a nullable
`light_direction` enum to the plant (one column, immediately useful — "show me everything in a
north-facing spot"), or promote spots to their own table (room has many spots, a spot has a
direction, a plant sits in a spot). The second is the honest model and would also stop spot
names drifting as free text, but it is a third level of hierarchy and a migration. Suggest the
column first; promote later if spots start repeating across rooms.

**Substrate type** — nullable enum on the plant (SOIL, SEMI_HYDRO, …). Worth noting for later:
semi-hydro is topped up rather than watered, so its rhythm differs, and the overdue rule may
eventually want a per-substrate threshold rather than one median rule for everything. Pairs
naturally with repotting events, since substrate is exactly what changes when repotting.

### Archiving plants

A plant that died, was trashed, sold or given away should leave the active list without losing
its history. Deleting stays available for a mistake or a duplicate entry, and becomes the loud,
confirmed path.

**Shape.** `archived_at TIMESTAMP NULL` on `plants` rather than a boolean: NULL means active,
and a timestamp both flags it and records when, which a boolean cannot. Plus `archived_reason`
as an enum — DIED, DISCARDED, SOLD, GIVEN_AWAY — for the same reason pest type should be an
enum: free text drifts into typos and ruins grouping. An archived plant keeps its room, its
waterings and its photos; that is the point.

**The real cost is not the column, it is every query that must now exclude archived plants.**
Miss one and a dead plant turns up as overdue. The list, as of today: `GET /plants`,
`countByRoom_Id` (which gates whether a room can be deleted — an archived plant must not block
it), `findByRoom_Id` behind the room stats, the overdue lists, the watering and bulk-watering
dropdowns, and the Analytics plant dropdown. Analytics for one archived plant should still be
*reachable*, since its history is the thing worth keeping; it just must never appear in
overdue. Expect a `findByArchivedAtIsNull` variant of most of those finders, and decide early
whether "archived" is a filter on the existing endpoints or separate ones.

**Deleting, as it stands today, is worse than it looks.** `deletePlant` in `app.js` has no
confirmation at all — one click on the ✕ and it is gone. `schema.sql` declares
`ON DELETE CASCADE` on both `watering_events` and `plant_images`, so every watering and every
photo row goes with it, silently and unrecoverably. The confirm should quantify what is at
stake, e.g. "Delete Monstera? This permanently removes 23 waterings and 4 photos." The photo
count is already in the frontend's `imagesByPlantId` cache; the watering count needs a fetch
or can come from the analytics `totalWaterings`.

**Also decide:** whether unarchiving is offered, and what a QR code for an archived plant
should do, since the printed codes outlive the plant.

---

## Why the code is the way it is

Decisions worth not relitigating, and the reasoning that justifies them.

**Room figures are one number per plant, not one per watering.** The room average is a mean of
per-plant means, and the room median is the median of the plants' medians. The question the
room figures answer is how often watering happens in this room, and a cactus is part of that
rhythm even though it is watered rarely. Pooling every gap instead would let a plant watered
fifty times outvote one watered twice.

**Both mean and median are reported, median first,** since the median is the rule the "Check"
line is judged by. `Medians.of` was extracted from `AnalyticsService` so the plant median and
the room median cannot drift apart.

**Overdue is measured against the median, not the mean,** and is computed on the server. One
forgotten stretch would otherwise raise the bar and hide a plant that is genuinely due. It
lives on `PlantAnalytics` rather than in the frontend because the substrate idea anticipates
changing the overdue rule, and the rule plus its reasoning should then live in one place.

**Never-watered plants sort to the top of a room's plant list,** so they are not forgotten.
That is the point of the ordering and it outweighs the cost: a cutting potted this morning also
sits above a genuinely thirsty plant until it is first watered. `Comparator.nullsFirst` in
`RoomService.statsFor`, pinned by `getRoomStats_sortsANeverWateredPlantFirst`.

**Several waterings on one day count as one.** A same-day top-up would otherwise add a zero gap
and drag the median down. Recording several waterings a day is not expected behaviour, but
grouping them is the logical reading of "the gap between waterings".

**The chart's time axis has a three-day floor** (`timeAxisWindow`), because two waterings hours
apart otherwise stretch to fill the axis and read as two separate days under `MMM d` ticks.

**The watering chart puts every watering in a gutter below zero** and the recorded amounts above
the zero line, which is drawn as the divider. A gutter mark with nothing above it is a watering
whose volume was never recorded. No connecting line, since joining amounts across waterings that
have none draws a slope between numbers that are not a series; spacing on the x axis carries the
intervals, so no second chart is needed. Fertiliser waterings are triangles.

**Moving a plant is a `<select>`, not drag and drop.** HTML5 drag and drop does not work on
touch, and the phone is a first-class client here thanks to the QR watering page, so a drag-only
move would be unusable where it is most wanted. A picker is also keyboard- and
screen-reader-accessible for free. Drag and drop on top of it is a reasonable follow-up — as an
addition, not a replacement. The picker is hidden when there is only one room, since there is
nowhere to move to.

**A room's plant list is closed by default,** since a room with a dozen plants pushed every
other room card off the screen. Which rooms are open lives in a `Set`, not in the markup: a move
re-renders every card and would otherwise collapse the room being worked in. The expander is
dropped entirely for an empty room.

**Room cards filter the already-loaded `/plants` response** rather than fetching per room, which
costs no extra request and keeps one source of truth for the plant cache. Fetching per room
would have meant one request per room card on every Rooms tab visit. `GET /rooms/{id}/plants`
was deleted for this reason.

**A watering round leaves no trace of being a round.** Bulk watering is expected to become the
norm, so a `round_id` would mark nearly every event and distinguish almost nothing, and the
per-room overdue list covers what the grouping would have been used for. No analytics are
affected — each plant still has its own event and timestamp.

**404 means the URL names something absent; 400 means the body does.** The rule the rooms work
settled on, and the reason the two queued `RuntimeException` fixes below need different
treatment from each other.

**The room selection persists after adding a plant.** Name, species and spot belong to the plant
just added; the room is the context being worked in, and several plants usually go into one room
in a sitting. Related: `lastRoom`'s only reader is `cancelNewRoom` — pick a room, pick
"+ New room…", Cancel, and the earlier choice comes back instead of a blank select.

**`escapeHtml` escapes quotes as well as `&`, `<` and `>`,** because interpolated text lands in
three different HTML contexts and each has a different character that ends it early:

- *Text position* (`<strong>${name}</strong>`) — `<` ends it, so `&`, `<`, `>` is complete.
  This is what the `textContent`/`innerHTML` round trip gives for free.
- *Attribute value* (`value="${name}"`) — `"` ends it, which is why quotes are escaped too. The
  room rename field is the only attribute in the codebase carrying user text.
- *Handler attribute* (`onclick="f('${name}')"`) — **no HTML escaping can make this safe.** A
  handler is parsed twice: the HTML parser decodes entities, *then* JS parses the result. So
  `&#39;` becomes a real `'` before JS ever sees it and terminates the string literal. Every
  inline handler in `app.js` interpolates a numeric id for exactly this reason. If user text
  ever has to reach a handler, pass the id and look the value up instead.

**Seed data does not run on startup.** `spring.sql.init.mode=always` is needed for `schema.sql`
and governed `data.sql` too, so the file had been reduced to `SELECT 1` with its contents
commented out. The scripts live in `seed/`, where nothing runs them, and their guards are per
name rather than "only while the table is empty" — which is what had made the old file useless
against a database that already had plants.

---

## Queued work on `main`

### Two bare `RuntimeException`s answer 500

Both predate the rooms branch and are reachable today. Spring maps an unannotated
`RuntimeException` to **500**, so a client asking about a plant that isn't there is told the
server broke:

- `PlantService.addImage` — `throw new RuntimeException("Plant not found")`, hit by
  `POST /plants/{id}/images` with an unknown id.
- `WateringService.recordWatering` — `.orElseThrow(() -> new RuntimeException("Plant not
  found"))`, hit by `POST /watering` with an unknown `plantId`.

**They need different statuses, so one exception type cannot serve both** — see the 404/400 rule
above.

- `addImage` takes the plant id from the **URL** → **404**. Add `PlantNotFoundException` with
  `@ResponseStatus(NOT_FOUND)` next to `RoomNotFoundException`.
- `recordWatering` takes `plantId` from the **body** → **400**. No new type needed: mirror its
  own sibling `recordWateringForPlants`, which already returns null for an unknown id while the
  controller turns that into `badRequest()`. A single null cause is not the ambiguity that was
  worth removing from `setRoom` — that one null stood for two causes.

Tests: `recordWatering_throwsWhenPlantNotFound` becomes a returns-null test, and add a
controller test for the 400 mirroring `recordWateringForPlants_returns400WhenAnyPlantIsUnknown`.
Nothing currently covers `addImage` with an unknown plant, so that wants a test too.

One commit for both sites, stating they are pre-existing bugs. If it starts pulling in a
`@ControllerAdvice` or a JSON error body, that is separate work — worth doing eventually, since
`@ResponseStatus` alone gives Spring's default error body with no message.

**Deliberately left alone:** `PlantService.deleteImage`'s bare `.orElseThrow()` is guarded by
`existsByIdAndPlantId` on the line above, and `AnalyticsService`'s `.average().orElseThrow()`
runs on a list that cannot be empty at that point. Both are invariant assertions, not reachable
errors.

### Deleting a plant orphans its photo files

`PlantService.deletePlant` only calls `plantRepository.deleteById`. The database cascades the
`plant_images` rows, but nothing deletes the files, so every photo of a deleted plant stays in
`./uploads/` forever. `deleteImage` does call `Files.deleteIfExists` — the single-photo path
cleans up, the whole-plant path does not.

---

## Known and accepted

Logged so they are not rediscovered as surprises.

- **`RoomService.statsFor` is N+1** — one analytics query per plant on every Rooms tab visit.
  The code comment already owns this and it is fine at this size.
- **Room name uniqueness is checked case-insensitively** (`existsByNameIgnoreCase`) but the
  `rooms.name` UNIQUE constraint is case-sensitive. The API behaves consistently; the database
  is just a weaker backstop than the code above it. Fine as-is, worth knowing before anyone
  relies on the constraint.
- **`savePlantLocation` fires two requests that are not atomic** — `PATCH /plants/{id}` for the
  spot, then `PUT /plants/{id}/room`. If the second fails the spot is already saved, the alert
  does not hint that half of it landed, and `loadPlants()` never runs, so the UI keeps showing
  the old spot that was in fact persisted. This is the wart the edit form removes properly: one
  form, one endpoint, one request. Deliberately not patched in isolation.
- **In `index.html`, the `analytics-stats` div is written `id="analytics-stats"style="…"`** with
  no space between the attributes. Browsers recover and it renders correctly, which is why it
  has gone unnoticed. Pre-existing, not a rooms issue. (Searched for rather than cited by line,
  since line numbers drift.)
