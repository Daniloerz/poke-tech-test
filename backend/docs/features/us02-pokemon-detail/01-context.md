# US02 — Pokemon Detail

## Feature

Detailed view of one Pokemon, read from PokeAPI.

## Goal

Let users open one Pokemon and see its image, core statistics, description and evolution chain.

## Functional context

The detail needs data from three PokeAPI endpoints, and each call depends on the previous one:

1. `GET /pokemon/{idOrName}`: image, stats, types, height, weight and the species name.
2. `GET /pokemon-species/{speciesName}`: description (`flavor_text_entries`) and the evolution chain URL.
3. `GET /evolution-chain/{id}`: the evolution tree.

The species can be different from the Pokemon: `deoxys-attack` belongs to species `deoxys`. The evolution chain is a tree, not a line: Eevee has 8 branches.

This feature reuses the PokeAPI client and the Redis cache of US01.

## Actors

- **Visitor:** any user, authenticated or not. The detail is a public route.

## User story

> As a visitor, I want to open a Pokemon, so that I can see its image, core statistics, description and evolution chain.

## Acceptance criteria

| ID | Criterion |
|---|---|
| AC-01 | `GET /api/v1/pokemon/{idOrName}` returns `200` with the detail. It accepts the PokeAPI id (`133`) or name (`eevee`). |
| AC-02 | The identifier is case-insensitive and trimmed: `Eevee` returns the same Pokemon as `eevee`. |
| AC-03 | The detail has `id`, `name`, `imageUrl`, `types`, `heightM`, `weightKg`, `stats`, `description` and `evolutionChain`. |
| AC-04 | `imageUrl` is the official artwork. If PokeAPI has no artwork, it is the sprite; if there is neither, it is `null`. |
| AC-05 | `stats` has the 6 base stats in PokeAPI order, each with `name` and `baseStat`. |
| AC-06 | `heightM` is the PokeAPI `height` (decimetres) divided by 10 (7 → 0.7). `weightKg` follows the US01 rule. |
| AC-07 | `description` is the most recent English description, with line breaks and form feeds replaced by single spaces. If there is no English description, it is `null`. |
| AC-08 | `evolutionChain` is a tree: each node has `id` (species id), `name`, `imageUrl` and `evolvesTo` (list of nodes, empty at the end of a branch). The root is the first stage of the chain, not the requested Pokemon. |
| AC-09 | A Pokemon without an evolution chain in PokeAPI is returned with `evolutionChain: null`. |
| AC-10 | An identifier that does not exist in PokeAPI returns `404` in the common error format. |
| AC-11 | An identifier with characters other than letters, digits and hyphens, or longer than 50 characters, returns `400` without calling PokeAPI. |
| AC-12 | If PokeAPI fails in any of the three calls, the API returns `502`. |
| AC-13 | The three PokeAPI responses are cached in Redis, reusing the US01 cache for the Pokemon. |

## Business rules

- Pokemon names are case-insensitive.
- Height is shown in metres and weight in kilograms.
- The evolution chain shows the whole family, starting from the first stage.

## Main flow

1. The visitor requests a Pokemon by id or name.
2. The API validates and normalizes the identifier.
3. The API gets the Pokemon, then its species, then the evolution chain (from the cache or PokeAPI).
4. The API returns the detail.

## Alternative flows

- No artwork: the sprite is used (AC-04).
- No English description: `description: null` (AC-07).
- No evolution chain: `evolutionChain: null` (AC-09).

## Error flows

- Invalid identifier: `400` (AC-11).
- Pokemon not found: `404` (AC-10).
- PokeAPI error, or species missing for an existing Pokemon (inconsistent data): `502` (AC-12).

## Dependencies

- US01: PokeAPI client, `Pokemon` domain model, Redis cache, error handler.

## Explicit assumptions

- "Core statistics" are the 6 PokeAPI base stats (`hp`, `attack`, `defense`, `special-attack`, `special-defense`, `speed`).
- "Image" is the official artwork (large image), different from the small sprite used in the list.
- "Most recent" description = the last English entry in `flavor_text_entries` (PokeAPI orders them by game version).
- Moves are not part of the detail; they are already in the list (US01).
- Evolution conditions (level, item, trade) are not shown. The story asks for the lineage only.
- Evolution node images are built from the species id with the PokeAPI sprites URL pattern (TDR-002).

## Non-functional requirements

- Same timeouts, cache TTL and error format as US01.
- A cached detail is served without calling PokeAPI.
