# US03 — Implementation Plan

## Endpoints (protected)

| Method | Path | Request | Response |
|---|---|---|---|
| `POST` | `/api/v1/local-pokemon` | `{ "idOrName": "pikachu" }` | `201` + `Location: /api/v1/local-pokemon/4` + `LocalPokemonResponse` |
| `GET` | `/api/v1/local-pokemon/{id}` | — | `200` `LocalPokemonResponse` |

`LocalPokemonResponse`:

```json
{
  "id": 4,
  "pokeApiId": 25,
  "name": "pikachu",
  "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
  "types": ["electric"],
  "heightM": 0.4,
  "weightKg": 6.0,
  "localizedName": null,
  "region": null,
  "tags": [],
  "syncedAt": "2026-10-03T23:00:00Z",
  "updatedAt": "2026-10-03T23:00:00Z"
}
```

Errors (Problem Details): `400`, `401`, `404`, `409` (detail includes the existing local id), `502`.

### Why a body instead of `POST /api/v1/local-pokemon/{idOrName}` (Phase 0 wording)

With the path version, `/api/v1/local-pokemon/{x}` would mean "PokeAPI id or name" for `POST` but "local id" for `GET` (and for `PUT`/`DELETE` in US04). The same URL shape with two meanings is confusing and error-prone. `POST` to the collection with the source in the body is the standard REST way to create a resource. This changes the Phase 0 wording and was approved by the user (TDR-001).

## Package structure (US01 ADR-001 applies)

```text
domain/model/                       LocalPokemon
application/
├── port/out/                       LocalPokemonRepository
├── exception/                      PokemonAlreadySyncedException, LocalPokemonNotFoundException
└── usecase/                        SyncPokemonUseCase, GetLocalPokemonUseCase
infrastructure/persistence/         LocalPokemonEntity, LocalPokemonJpaRepository, LocalPokemonEntityMapper,
                                    LocalPokemonRepositoryAdapter
interfaces/rest/                    LocalPokemonController, dto/ (SyncPokemonRequest, LocalPokemonResponse),
                                    mapper/ (LocalPokemonRestMapper)
```

## Flow

```text
POST: LocalPokemonController → SyncPokemonUseCase.sync(idOrName)          (no transaction here, TDR-002)
        → normalize identifier (same rule as US02)
        → PokemonCatalogPort.findByIdOrName        empty → PokemonNotFoundException (404)
        → LocalPokemonRepository.findByPokeApiId   present → PokemonAlreadySyncedException (409)
        → LocalPokemon.fromCatalog(pokemon) → LocalPokemonRepository.save → 201

GET:  LocalPokemonController → GetLocalPokemonUseCase.get(id)
        → LocalPokemonRepository.findById           empty → LocalPokemonNotFoundException (404)
```

The PokeAPI call is outside any database transaction, so no database connection is held while waiting for PokeAPI (up to the 5 s read timeout). The "already local" check is a plain read, and the insert has its own transaction; the unique constraint on `poke_api_id` is the final guard if two requests synchronize the same Pokemon at the same time (TDR-002).

## Domain

- `LocalPokemon` (record, `@Builder`): `id`, `pokeApiId`, `name`, `spriteUrl`, `types`, `heightDecimetres`, `weightHectograms`, `localizedName`, `region`, `tags`, `syncedAt`, `updatedAt`.
  - `static LocalPokemon fromCatalog(Pokemon pokemon)`: copies the PokeAPI snapshot; proprietary fields empty.
  - `heightM()`, `weightKg()`: same conversions as `Pokemon`. Both records use one small package-private helper (`Measurements`) so the rule lives in one place.
  - Null lists become empty immutable lists.
- Identifier normalization (`trim` + lower case) moves from `GetPokemonDetailUseCase` to a domain helper (`PokemonIdentifier.normalize`), shared by US02 and US03.

## Application

- `LocalPokemonRepository` (out port): `Optional<LocalPokemon> findById(long)`, `Optional<LocalPokemon> findByPokeApiId(int)`, `LocalPokemon insert(LocalPokemon)`.
  - `insert` runs in its own transaction (`saveAndFlush`). Only a violation of `uk_local_pokemon_poke_api_id` (two synchronizations at the same time) becomes `PokemonAlreadySyncedException`; any other integrity error is rethrown (`500`), so it is never hidden behind a wrong `409`.
- `SyncPokemonUseCase`, `GetLocalPokemonUseCase` (ADR-001).
- `PokemonAlreadySyncedException(name, localId)` → `409`, detail `Pokemon already synchronized: pikachu (local id 4)`.
- `LocalPokemonNotFoundException(id)` → `404`, detail `Local Pokemon not found: 99`.

## Infrastructure

- Liquibase:
  - `20261003-03-create-local-pokemon.sql`
  - `20261003-04-seed-local-pokemon.sql` (Bulbasaur, Charmander, Squirtle with proprietary fields).
- `LocalPokemonEntity` (`@Entity`, table `local_pokemon`):
  - `types` and `tags` as `List<String>` mapped to PostgreSQL `text[]` with `@JdbcTypeCode(SqlTypes.ARRAY)` (BDDR-007).
  - `syncedAt` filled by the database default (`@Generated`), `updatedAt` by Hibernate (`@UpdateTimestamp(source = SourceType.DB)`, also set on insert). Both use the database clock: with the JVM clock, `updatedAt` could be a few milliseconds earlier than `syncedAt` (seen during the manual check).
- `LocalPokemonJpaRepository extends JpaRepository<LocalPokemonEntity, Long>` with `findByPokeApiId`.
- `LocalPokemonEntityMapper` (MapStruct), `LocalPokemonRepositoryAdapter`.

## Interfaces (REST)

- `LocalPokemonController` (`/api/v1/local-pokemon`), OpenAPI annotations with `bearerAuth`.
- `SyncPokemonRequest(@NotBlank @Pattern("^[A-Za-z0-9-]{1,50}$") String idOrName)`.
- `LocalPokemonResponse`, `LocalPokemonRestMapper` (MapStruct; `heightM()`/`weightKg()` by expression).
- `GlobalExceptionHandler`: `PokemonAlreadySyncedException` → `409`, `LocalPokemonNotFoundException` → `404`.
- Security: no change; `/api/v1/local-pokemon/**` is protected by the default rule.

## Configuration and dependencies

- No new configuration. No new dependencies.

## Tests (unit only)

| AC | Tests |
|---|---|
| AC-01, AC-03 | `SyncPokemonUseCaseTest` (copies the snapshot, empty proprietary fields, normalization), `LocalPokemonTest` (`fromCatalog`, conversions) |
| AC-02 | `LocalPokemonControllerTest` (JSON shape, `Location`) |
| AC-04 | `SyncPokemonUseCaseTest` (existing `pokeApiId`, also when requested by name), `LocalPokemonRepositoryAdapterTest` (unique violation → `409` exception) |
| AC-05, AC-07 | `SyncPokemonUseCaseTest` (not found, PokeAPI error → nothing saved) |
| AC-06 | `LocalPokemonControllerTest` (blank, invalid characters, too long, malformed JSON) |
| AC-08, AC-11 | `LocalPokemonControllerTest` (no token → `401`) |
| AC-09, AC-10 | `GetLocalPokemonUseCaseTest`, `LocalPokemonControllerTest` (`200`, `404`, non-numeric id `400`) |
| AC-12 | Manual check with Docker Compose |
| — | `LocalPokemonRepositoryAdapterTest` (entity ↔ domain mapping of arrays and times; only the unique constraint becomes `409`) |

The entity mapping (`text[]`, generated timestamps) and the changesets are checked with Docker Compose (database tests are backlog item 1).

Manual checks with Docker Compose: Liquibase applied the two new changesets on an existing volume; seeded Bulbasaur with its proprietary fields; `Pikachu` → `201` + `Location: /api/v1/local-pokemon/4` with empty proprietary fields; again by id `25` → `409 (local id 4)`; `missingno` → `404`; `mr.mime` → `400`; no token → `401`; `GET /abc` → `400`, `GET /999` → `404`; **two parallel synchronizations of `eevee` → one `201` and one `409`**, one row in the table; `types` and `tags` stored as `text[]`; `syncedAt` equals `updatedAt` after the insert.

## Security

- Protected routes; input validated with the same pattern as US02.
- The `409` detail echoes the validated name and the local id only.

## Documentation to update

- `docs/bd/`: `erd.mmd`, `ddl.sql`, `dml.sql`, `bddr.md` (BDDR-007..009).
- README: endpoints, status.
