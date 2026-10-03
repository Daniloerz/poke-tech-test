# US01 — Pokemon List

## Feature

PokeAPI integration and paginated Pokemon list.

## Goal

Let users browse Pokemon page by page. Each entry shows its sprite, category (types), weight and skills (moves). The PokeAPI responses are cached in Redis.

## Functional context

PokeAPI exposes the list in `GET /pokemon?offset&limit`, but that endpoint only returns `name` and `url` for each Pokemon. The sprite, types, weight and moves are only in `GET /pokemon/{name}`. Because of that, one page of our list needs **1 + N** calls to PokeAPI. Calls are done in parallel and cached, so the list stays fast.

This feature also builds the PokeAPI client that US02 will reuse.

## Actors

- **Visitor:** any user, authenticated or not. The list is a public route.

## User story

> As a visitor, I want to browse Pokemon in pages, so that I can see each Pokemon's sprite, category, weight and skills without loading the whole catalog.

## Acceptance criteria

| ID | Criterion |
|---|---|
| AC-01 | `GET /api/v1/pokemon?page=0&size=20` returns `200` with the first 20 Pokemon, in PokeAPI order (by id). |
| AC-02 | Each item has `id`, `name`, `spriteUrl`, `types` (list of type names), `weightKg` and `moves` (list of move names). |
| AC-03 | `weightKg` is the PokeAPI `weight` (hectograms) divided by 10, with one decimal (69 → 6.9). |
| AC-04 | The response has the pagination data: `page`, `size`, `totalElements`, `totalPages`. |
| AC-05 | If `page` and `size` are missing, the defaults are `page=0` and `size=20`. |
| AC-06 | `page < 0`, `size < 1`, `size > 50` or a non-numeric value returns `400` with an error body in the common error format. |
| AC-07 | A page after the last one returns `200` with an empty `content` and the real `totalElements`. |
| AC-08 | If PokeAPI fails (5xx, timeout, connection error) for the list or for any Pokemon of the page, the API returns `502` with the common error format, without internal details. |
| AC-09 | A Pokemon without a sprite in PokeAPI is returned with `spriteUrl: null`; it does not break the page. |
| AC-10 | PokeAPI responses are cached in Redis. A second request for the same page does not call PokeAPI again while the cache entry is valid. |
| AC-11 | If Redis is not available, the list still works (it calls PokeAPI directly), a warning is logged, and `/actuator/health` stays `UP`. |

## Business rules

- Page size is limited to 50 to control the number of parallel calls to PokeAPI.
- The order of the items is the PokeAPI order. Parallel calls must not change it.
- Weight is shown in kilograms.

## Main flow

1. The visitor requests a page.
2. The API validates `page` and `size`.
3. The API gets the page of names from PokeAPI (or from the cache).
4. For each name, the API gets the Pokemon data from PokeAPI (or from the cache), in parallel.
5. The API returns the page in the original order.

## Alternative flows

- Page after the last one: empty `content` (AC-07).
- Data already in the cache: no call to PokeAPI (AC-10).
- Redis down: the cache is skipped (AC-11).

## Error flows

- Invalid parameters: `400` (AC-06).
- PokeAPI error, timeout or invalid response: `502` (AC-08).
- Unexpected error: `500` with a generic message.

## Dependencies

- None. US02 depends on the PokeAPI client of this feature.

## Explicit assumptions

- "Category" = PokeAPI `types` and "skills" = `moves` (decision approved in Phase 0).
- All the moves of each Pokemon are returned. The frontend decides how many to show.
- `totalElements` is the PokeAPI `count`. It includes alternative forms (ids over 10000), because PokeAPI lists them in the same endpoint.
- Names, types and moves are returned as PokeAPI gives them (English, lower case with hyphens).
- The list is public. Authentication arrives in F3 and does not change this route.

## Non-functional requirements

- PokeAPI calls have a connection timeout and a read timeout, both configurable.
- The cache has a configurable TTL. The default is 24 hours, because PokeAPI data almost never changes.
- No PokeAPI payloads in the logs; only the call and the error.
