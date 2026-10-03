# US03 — Data Synchronization

## Feature

Copy a Pokemon from PokeAPI into the local PostgreSQL database, where it can get proprietary fields.

## Goal

Keep a local, editable copy of selected Pokemon. The local copy has the main PokeAPI data plus fields that PokeAPI does not have: a localized name, geographic metadata and internal classification tags.

## Functional context

The exercise says: "Develop a mechanism to persist Pokemon data into a local relational store. This replication layer is intended to facilitate the addition of proprietary fields. Use cases include localized nomenclature, geographical metadata, or internal classification tags."

Approved in Phase 0:

- Synchronization is **on demand, one Pokemon at a time**, by PokeAPI id or name. A Pokemon that is already local returns `409`.
- The local copy stores name, height, weight, sprite and types, plus the proprietary fields. Moves are not stored.
- Creating Pokemon that do not exist in PokeAPI is a backlog item; for now, local Pokemon are only created by synchronization.
- Local data routes are protected (authenticated user).

This feature creates the local Pokemon table and the "create" and "read one" operations. US04 adds the list, update and delete operations on the same data.

## Actors

- **Authenticated user:** synchronizes and reads local Pokemon.

## User story

> As an authenticated user, I want to copy a Pokemon from PokeAPI into the local database, so that I can later add our own data to it (localized name, region, tags).

## Acceptance criteria

### Synchronize — `POST /api/v1/local-pokemon`

| ID | Criterion |
|---|---|
| AC-01 | Body `{"idOrName": "pikachu"}` (id or name, case-insensitive) copies the Pokemon from PokeAPI and returns `201` with the local record and `Location: /api/v1/local-pokemon/{id}`. |
| AC-02 | The local record has: local `id`, `pokeApiId`, `name`, `spriteUrl`, `types`, `heightM`, `weightKg`, the proprietary fields `localizedName`, `region`, `tags`, and the audit fields `syncedAt`, `updatedAt`. |
| AC-03 | After synchronization the proprietary fields are empty: `localizedName` and `region` are `null`, `tags` is `[]`. They are filled with US04. |
| AC-04 | A Pokemon that is already local (same `pokeApiId`, requested by id or by name) returns `409` and does not change the stored record. The response says the local id of the existing record. |
| AC-05 | An identifier that does not exist in PokeAPI returns `404`. |
| AC-06 | A missing, blank or invalid `idOrName` (characters other than letters, digits and hyphens, or more than 50) returns `400`. A malformed body returns `400`. |
| AC-07 | If PokeAPI is not available, the API returns `502` and nothing is stored. |
| AC-08 | Without a valid token the API returns `401`. |

### Read one — `GET /api/v1/local-pokemon/{id}`

| ID | Criterion |
|---|---|
| AC-09 | Returns `200` with the local record (same shape as AC-02). |
| AC-10 | An id that does not exist locally returns `404`. A non-numeric id returns `400`. |
| AC-11 | Without a valid token the API returns `401`. |

### Data

| ID | Criterion |
|---|---|
| AC-12 | The database starts with three local Pokemon for the demo (Bulbasaur, Charmander, Squirtle) with proprietary fields filled in. |

## Business rules

- One local record per PokeAPI Pokemon (`pokeApiId` is unique).
- The PokeAPI data is a snapshot taken at synchronization time.
- Proprietary fields belong only to the local copy; PokeAPI is never modified.

## Main flow

1. The user sends `idOrName`.
2. The API validates and normalizes it, and reads the Pokemon from PokeAPI (or the Redis cache of US01/US02).
3. If the Pokemon is not local yet, the API stores it and returns `201`.

## Alternative flows

- The Pokemon is in the Redis cache: no call to PokeAPI.

## Error flows

- Invalid input `400`; no token `401`; not in PokeAPI `404`; already local `409`; PokeAPI down `502`.

## Dependencies

- US02: `PokemonCatalogPort.findByIdOrName` and the identifier rules.
- User authentication: protected routes.

## Explicit assumptions

- The proprietary fields are edited only with US04; the synchronization request has no proprietary fields. This keeps one place that validates them.
- `localizedName` is free text (the user's own name for the Pokemon, in any language). `region` is free text (for example `Kanto`, or a real-world location used by the business). `tags` are short free-text labels.
- Re-synchronizing (refreshing the PokeAPI snapshot) is out of scope; `409` protects the local data.
- The sprite URL is stored, not the image itself.

## Non-functional requirements

- The check "already local" and the insert run in one transaction; a unique constraint protects against two synchronizations at the same time.
- No PokeAPI payloads in the logs.
