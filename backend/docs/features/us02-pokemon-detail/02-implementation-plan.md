# US02 — Implementation Plan

## Endpoint

`GET /api/v1/pokemon/{idOrName}` — public.

| Parameter | Validation |
|---|---|
| `idOrName` (path) | `^[A-Za-z0-9-]{1,50}$`, then trimmed and lower-cased by the use case |

Response `200`:

```json
{
  "id": 133,
  "name": "eevee",
  "imageUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/133.png",
  "types": ["normal"],
  "heightM": 0.3,
  "weightKg": 6.5,
  "stats": [
    { "name": "hp", "baseStat": 55 },
    { "name": "attack", "baseStat": 55 }
  ],
  "description": "Harbors the potential to evolve into manifold forms. ...",
  "evolutionChain": {
    "id": 133,
    "name": "eevee",
    "imageUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/133.png",
    "evolvesTo": [
      { "id": 134, "name": "vaporeon", "imageUrl": "...", "evolvesTo": [] }
    ]
  }
}
```

Errors (Problem Details, US01 TDR-004): `400` invalid identifier (with `errors`), `404` not found, `502` PokeAPI not available.

## Architecture

No new architectural decision: US01 ADR-001 (layers, out ports) and ADR-002 (cache in the adapter) apply as they are. So this feature has no `03-adrs.md`.

## Flow

```text
PokemonController.getDetail(idOrName)
  → GetPokemonDetailUseCase.get(idOrName)
      → normalize (trim + lower case)
      → PokemonCatalogPort.findByIdOrName(id)      [cached: pokeapi-pokemon]   empty → PokemonNotFoundException (404)
      → PokemonCatalogPort.findSpecies(species)    [cached: pokeapi-species]   empty → ExternalServiceException (502)
      → PokemonCatalogPort.findEvolutionChain(id)  [cached: pokeapi-evolution-chains], only if the species has one
      → PokemonDetail
  → PokemonRestMapper → PokemonDetailResponse
```

The three calls are sequential because each one needs data from the previous one.

## Domain (`domain/model`)

- `Pokemon` (existing record) gains: `heightDecimetres`, `artworkUrl`, `stats` (`List<PokemonStat>`), `speciesName`. New methods: `heightM()` (AC-06) and `imageUrl()` (artwork, else sprite; AC-04). The list (US01) keeps using `spriteUrl`. With 10 components, the record uses Lombok `@Builder` (compile time only) so tests and mappers stay readable.
- `PokemonStat` (record): `name`, `baseStat`.
- `PokemonSpecies` (record): `name`, `description` (nullable), `evolutionChainId` (nullable `Integer`).
- `EvolutionNode` (record): `speciesId`, `name`, `imageUrl`, `evolvesTo` (`List<EvolutionNode>`, never null).
- `PokemonDetail` (record): `pokemon`, `description`, `evolutionChain` (nullable).

## Application

- `PokemonCatalogPort`:
  - `findByName` is renamed `findByIdOrName(String idOrName)`, because it now also receives ids (PokeAPI accepts both).
  - `Optional<PokemonSpecies> findSpecies(String speciesName)`
  - `Optional<EvolutionNode> findEvolutionChain(int evolutionChainId)`
- `PokemonNotFoundException` (`application/exception`): the requested Pokemon does not exist (`404`).
- `GetPokemonDetailUseCase`: normalizes the identifier, runs the flow above, builds `PokemonDetail`. A species that is missing for an existing Pokemon is inconsistent data from PokeAPI → `ExternalServiceException` (`502`). A chain id that returns 404 → `evolutionChain: null` and a `WARN` log.

## Infrastructure

- `PokeApiClient`:
  - `findSpecies` → `GET /pokemon-species/{name}`, `@Cacheable(pokeapi-species)`.
  - `findEvolutionChain` → `GET /evolution-chain/{id}`, `@Cacheable(pokeapi-evolution-chains)`.
  - Same `get` helper as US01 (404 → empty, other errors → `ExternalServiceException`).
- New DTOs: `PokeApiSpeciesResponse` (`flavor_text_entries`, `evolution_chain`), `PokeApiFlavorText`, `PokeApiEvolutionChainResponse`, `PokeApiChainLink` (recursive). `PokeApiPokemonResponse` gains `height`, `stats`, `species` and `sprites.other.official-artwork.front_default`.
- `PokeApiMapper` gains:
  - Stats: `stats[].stat.name` + `base_stat`.
  - Description (TDR-003): last English entry, whitespace normalized.
  - Evolution tree (TDR-001): recursive mapping of `chain`; the species id is read from the end of the species URL; the node image is built from that id (TDR-002).
  - Chain id: read from the end of `evolution_chain.url`. A URL without a numeric id is invalid PokeAPI data → `ExternalServiceException` (`502`).
- Both mappers declare `componentModel = SPRING` in `@Mapper` (explicit, instead of a global compiler option that produced a build warning).
- `PokeApiProperties` gains `spriteBaseUrl` (`POKEAPI_SPRITE_BASE_URL`, default `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon`).
- `CacheConfig`: two new caches (`pokeapi-species`, `pokeapi-evolution-chains`) and a version in the key prefix (TDR-004).

The node image needs the sprite base URL (a property). The client passes it to the mapper as a normal method parameter (`toEvolutionChain(response, spriteBaseUrl)`). The species, description and evolution mappings are hand-written `default` methods (MapStruct `@Context` is not needed), so the mapper stays an interface without field injection.

## Interfaces (REST)

- `PokemonController.getDetail`: one `@Pattern("^[A-Za-z0-9-]{1,50}$")` on the path variable covers characters and length (built-in method validation, as in US01). OpenAPI annotations for `200`, `400`, `404`, `502`.
- DTOs: `PokemonDetailResponse`, `PokemonStatResponse`, `EvolutionNodeResponse`.
- `PokemonRestMapper` gains the detail mapping (`heightM()`, `weightKg()`, `imageUrl()` through expressions).
- `GlobalExceptionHandler`: `PokemonNotFoundException` → `404` with detail `Pokemon not found: <identifier>`. The identifier is already validated, so it is safe to echo.

## Configuration

| Property | Env variable | Default |
|---|---|---|
| `pokeapi.sprite-base-url` | `POKEAPI_SPRITE_BASE_URL` | `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon` |

## Tests and traceability (unit only)

| AC | Tests |
|---|---|
| AC-01, AC-02 | `GetPokemonDetailUseCaseTest` (by id, by name, normalization), `PokemonControllerTest` |
| AC-03 | `PokemonControllerTest` (JSON shape) |
| AC-04, AC-06 | `PokemonTest` (`imageUrl()` fallback, `heightM()`) |
| AC-05 | `PokeApiMapperTest` (stats order) |
| AC-07 | `PokeApiMapperTest` (last English entry, whitespace, no English entry) |
| AC-08 | `PokeApiMapperTest` (Eevee-like branches, nested stages, species id, node image), `PokemonControllerTest` |
| AC-09 | `GetPokemonDetailUseCaseTest`, `PokeApiMapperTest` (no chain URL) |
| AC-10 | `GetPokemonDetailUseCaseTest`, `PokemonControllerTest` (`404`) |
| AC-11 | `PokemonControllerTest` (invalid characters, too long; no use case call) |
| AC-12 | `PokeApiClientTest` (new endpoints), `GetPokemonDetailUseCaseTest` (missing species), `PokemonControllerTest` (`502`) |
| AC-13 | Manual check with Docker Compose: Eevee cold 1.1 s, cached 22 ms; keys `poke-tech-test::v2::pokeapi-{pokemon,species,evolution-chains}::*`. Automated in backlog item 1 |

Other manual checks with Docker Compose: Eevee (8 branches, clean description, official artwork), `265` Wurmple (nested branches), `deoxys-attack` (species `deoxys`), `missingno` → `404`, `bad_name` → `400`, the US01 list still works after the model change, and a node sprite URL (`.../pokemon/267.png`) answers `200`.

## Security

- The identifier is validated before use: no path injection into PokeAPI URLs (Spring also encodes URI variables).
- The `404` detail echoes only the validated identifier.

## Performance

- Cold detail: 3 sequential PokeAPI calls. Cached detail: 3 Redis reads.
- A Pokemon already listed in US01 is reused from the cache (same key when requested by name).
- Requests by id and by name create two cache entries for the same Pokemon. This is accepted: the cache stays simple and the extra data is small.
