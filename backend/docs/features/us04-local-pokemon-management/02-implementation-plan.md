# US04 — Implementation Plan

## Endpoints (protected)

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/api/v1/local-pokemon?page=0&size=20` | — | `200` `PageResponse<LocalPokemonResponse>` |
| `PUT` | `/api/v1/local-pokemon/{id}` | `UpdateLocalPokemonRequest` | `200` `LocalPokemonResponse` |
| `DELETE` | `/api/v1/local-pokemon/{id}` | — | `204` |

`UpdateLocalPokemonRequest`:

```json
{ "localizedName": "Pikachu", "region": "Kanto", "tags": ["electric-mouse", "mascot"] }
```

Unknown field (AC-07):

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request parameters.",
  "errors": [{ "field": "name", "message": "is not an editable field" }]
}
```

## Architecture

No new architectural decision: US01 ADR-001 and US03 ADR-001 apply. No `03-adrs.md`.

## Flows

```text
GET list: LocalPokemonController → ListLocalPokemonUseCase.list(page, size)
            → LocalPokemonRepository.findPage(page, size)   (ORDER BY id, one query + one count)

PUT:      LocalPokemonController → UpdateLocalPokemonUseCase.update(id, changes)   @Transactional
            → LocalPokemonRepository.findById       empty → LocalPokemonNotFoundException (404)
            → localPokemon.withProprietaryFields(localizedName, region, tags)       (domain normalizes)
            → LocalPokemonRepository.update → 200

DELETE:   LocalPokemonController → DeleteLocalPokemonUseCase.delete(id)            @Transactional
            → LocalPokemonRepository.findById       empty → 404
            → LocalPokemonRepository.deleteById → 204
```

No external call in these flows, so the use cases can be `@Transactional` (unlike US03 TDR-002).

## Domain

- `PageResult<T>` (generic record): `items`, `page`, `size`, `totalElements`, `totalPages()`. It replaces `PokemonPage` (US01), so the catalog and the local list share one page model (TDR-003).
- `LocalPokemon.withProprietaryFields(String localizedName, String region, List<String> tags)`: returns a copy with the new values; trims the texts (blank becomes `null`), normalizes tags (trim, lower case, no duplicates, first order kept). The snapshot fields are copied unchanged.
- `ProprietaryFields` is not a separate type: three parameters are clearer here than a new record.

## Application

- `LocalPokemonRepository` gains: `PageResult<LocalPokemon> findPage(int page, int size)`, `LocalPokemon update(LocalPokemon)`, `void deleteById(long id)`.
- `ListLocalPokemonUseCase`, `UpdateLocalPokemonUseCase`, `DeleteLocalPokemonUseCase`.

## Infrastructure

- `LocalPokemonRepositoryAdapter`:
  - `findPage`: `PageRequest.of(page, size, Sort.by("id"))`, mapped to `PageResult`.
  - `update`: `saveAndFlush` of the mapped entity (Hibernate merge). `syncedAt` is not updatable; `updatedAt` is refreshed from the database clock.
  - `deleteById`: Spring Data `deleteById`.
- No schema change.

## Interfaces (REST)

- `LocalPokemonController` gains `list`, `update`, `delete` (OpenAPI annotations, `bearerAuth`).
- `UpdateLocalPokemonRequest` (record) with Bean Validation (TDR-002):
  - `localizedName`, `region`: `@Size(max = 100)`, `@Pattern(".*\\S.*")` (not blank when present).
  - `tags`: `@NotNull @Size(max = 10) List<@NotBlank @Pattern("^[A-Za-z0-9-]{1,30}$") String>`.
  - `@JsonIgnoreProperties(ignoreUnknown = false)`: unknown fields fail.
- `PageResponse<T>` reused; `LocalPokemonRestMapper` gains the page mapping. `PokemonRestMapper` uses `PageResult`.
- `GlobalExceptionHandler.handleHttpMessageNotReadable`: if the cause is an unknown property, the `400` lists it in `errors` (`"is not an editable field"`); otherwise "Malformed request body." Validation errors on list elements (`tags[1]`) keep their index in `field`.

## Tests (unit only)

| AC | Tests |
|---|---|
| AC-01, AC-02 | `ListLocalPokemonUseCaseTest`, `LocalPokemonRepositoryAdapterTest` (page request and mapping), `LocalPokemonControllerTest` (shape, defaults, `400`) |
| AC-03, AC-04, AC-05, AC-06 | `LocalPokemonTest` (`withProprietaryFields`: trim, blank → null, tag normalization, snapshot unchanged), `UpdateLocalPokemonUseCaseTest`, `LocalPokemonControllerTest` |
| AC-05, AC-06, AC-07, AC-08, AC-09 | `LocalPokemonControllerTest` (blank text, too long, too many tags, invalid tag, missing `tags`, unknown field, malformed body, `415`, `404`, non-numeric id) |
| AC-10, AC-11 | `DeleteLocalPokemonUseCaseTest`, `LocalPokemonControllerTest` (`204`, `404`) |
| AC-12 | `LocalPokemonControllerTest` (no token) |
| — | `PageResultTest` (replaces `PokemonPageTest`) |

Manual check with Docker Compose: full CRUD flow, `updatedAt` changes and `syncedAt` does not, delete and re-synchronize.

## Security

- Protected routes; strict request bodies (unknown fields rejected); size limits on every text and list.
