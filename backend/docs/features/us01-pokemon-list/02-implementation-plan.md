# US01 — Implementation Plan

## Endpoint

`GET /api/v1/pokemon?page={page}&size={size}` — public.

| Parameter | Type | Default | Validation |
|---|---|---|---|
| `page` | int | 0 | `>= 0` |
| `size` | int | 20 | `1..50` |

Response `200`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "bulbasaur",
      "spriteUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
      "types": ["grass", "poison"],
      "weightKg": 6.9,
      "moves": ["razor-wind", "swords-dance"]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1351,
  "totalPages": 68
}
```

Error response (`400`, `502`, `500`), RFC 9457 Problem Details (see TDR-004):

```json
{
  "title": "Bad Gateway",
  "status": 502,
  "detail": "The Pokemon service is not available. Try again later.",
  "instance": "/api/v1/pokemon"
}
```

`400` responses also include an `errors` list:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request parameters.",
  "instance": "/api/v1/pokemon",
  "errors": [{ "field": "size", "message": "must be less than or equal to 50" }]
}
```

## Package structure (see ADR-001)

```text
com.poketechtest
├── domain/model/                 Pokemon, PokemonPage                 (no framework code)
├── application/
│   ├── port/out/                 PokemonCatalogPort (interface), PokemonCatalogPage
│   ├── exception/                ExternalServiceException
│   └── usecase/                  ListPokemonUseCase
├── infrastructure/
│   ├── pokeapi/                  PokeApiClient (adapter), PokeApiMapper
│   │   └── dto/                  PokeApi*Response records
│   └── config/                   PokeApiConfig, PokeApiProperties, CacheConfig, CacheNames
└── interfaces/rest/              PokemonController, GlobalExceptionHandler
    ├── dto/                      PageResponse, PokemonSummaryResponse
    └── mapper/                   PokemonRestMapper
```

## Flow

```text
PokemonController
  → ListPokemonUseCase.list(page, size)
      → PokemonCatalogPort.listPage(offset, limit)          [cached: pokeapi-pages]
      → for each name, in parallel:
          PokemonCatalogPort.findByIdOrName(name)           [cached: pokeapi-pokemon]
      → PokemonPage (original order)
  → PokemonRestMapper → PageResponse<PokemonSummaryResponse>
```

## Domain

- `Pokemon` (record): `id`, `name`, `spriteUrl`, `types`, `weightHectograms`, `moves`. Method `weightKg()` converts to kilograms with `BigDecimal` (AC-03). Null lists become empty, immutable lists. US02 will add fields to this record.
- `PokemonPage` (record): `items`, `page`, `size`, `totalElements`, and `totalPages()` computed from them. It rejects `page < 0` and `size < 1`.

## Application

- `PokemonCatalogPort` (out port):
  - `PokemonCatalogPage listPage(long offset, int limit)`: one PokeAPI page of names and the total count.
  - `Optional<Pokemon> findByIdOrName(String idOrName)` (named `findByName` until US02, which added lookups by id): empty when PokeAPI answers 404 (`findXxx` returns `Optional`, global Java rule). US02 will use the empty case for its `404`.
  - Both throw `ExternalServiceException` when PokeAPI is not available.
- `PokemonCatalogPage` (record): `names` and `totalCount`. It is the result of the port, not a domain concept.
- `ListPokemonUseCase`:
  - Computes `offset = (long) page * size`, so a very large `page` cannot overflow `int`.
  - Calls `listPage`, then `findByIdOrName` for each name with `CompletableFuture.supplyAsync(..., executor)` on a virtual-thread executor (TDR-002).
  - Joins the futures in list order. If a future fails, it rethrows the original cause.
  - A listed name that `findByIdOrName` cannot find is an inconsistent catalog: `ExternalServiceException` (`502`).
- `ExternalServiceException`: business exception for "external catalog not available". The use case does not know about HTTP.

## Infrastructure

- `PokeApiConfig`:
  - `HttpClient` (JDK) with the connection timeout, closed when the app stops.
  - `RestClient` with the base URL and a `JdkClientHttpRequestFactory` with the read timeout (TDR-001).
  - `ExecutorService` bean with virtual threads, closed when the app stops.
- `PokeApiProperties` (`@ConfigurationProperties("pokeapi")`, validated at startup).
- `PokeApiClient` implements `PokemonCatalogPort`:
  - `GET /pokemon?offset&limit` and `GET /pokemon/{name}`.
  - `@Cacheable(pokeapi-pages, key = "offset-limit")` and `@Cacheable(pokeapi-pokemon, key = name, unless = result == null)`: a 404 is not cached.
  - One private `get` helper: 404 → `Optional.empty()`; empty body or any other `RestClientException` (4xx/5xx, timeout, invalid JSON) → `WARN` log with the operation and `ExternalServiceException`. No payloads in the logs.
- PokeAPI DTOs as records with only the fields we use (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- `PokeApiMapper` (MapStruct): DTO → domain, including the nested lists (`types[].type.name`, `moves[].move.name`, `sprites.front_default`).
- `CacheConfig` (TDR-003): `RedisCacheManager` with one JSON serializer per cache, TTL from properties, key prefix `poke-tech-test::`, no caches created on the fly. Spring's `LoggingCacheErrorHandler` logs cache errors as `WARN` and the call continues (AC-11).

## Interfaces (REST)

- `PokemonController`: `@Min/@Max` on the parameters. The class is **not** annotated with `@Validated`: Spring MVC built-in method validation runs and throws `HandlerMethodValidationException` (with `@Validated`, an AOP proxy would throw `ConstraintViolationException` instead). OpenAPI annotations for the `200`, `400` and `502` responses.
- `PokemonSummaryResponse`, `PageResponse<T>` (records).
- `PokemonRestMapper` (MapStruct): domain → response.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, extends `ResponseEntityExceptionHandler`):
  - `HandlerMethodValidationException` and type mismatch → `400` with `errors` (`field`, `message`).
  - `ExternalServiceException` → `502` (the adapter already logged the cause).
  - Any other `Exception` → `500` with a generic message; the stack trace goes only to the log (`ERROR`).

## Configuration

| Property | Env variable | Default |
|---|---|---|
| `pokeapi.base-url` | `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` |
| `pokeapi.connect-timeout` | `POKEAPI_CONNECT_TIMEOUT` | `3s` |
| `pokeapi.read-timeout` | `POKEAPI_READ_TIMEOUT` | `5s` |
| `pokeapi.cache-ttl` | `POKEAPI_CACHE_TTL` | `24h` |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` |
| `spring.data.redis.port` | `REDIS_PORT` | `6380` |
| `spring.data.redis.timeout` / `connect-timeout` | — | `1s` (fast fallback when Redis is down) |
| `management.health.redis.enabled` | — | `false` (Redis is only a cache; the app is healthy without it) |

## New dependencies

| Dependency | Reason |
|---|---|
| `spring-boot-starter-data-redis` | Redis connection for the cache. |
| `spring-boot-starter-cache` | Spring cache abstraction (`@Cacheable`). |
| `springdoc-openapi-starter-webmvc-ui` 3.1.1 | OpenAPI docs and Swagger UI (TDR-005). |

## Tests and traceability (unit only, decision 12)

| AC | Test (automated) | Manual check with Docker Compose |
|---|---|---|
| AC-01 | `ListPokemonUseCaseTest` (offset, order), `PokemonControllerTest` | First 20 Pokemon in PokeAPI order |
| AC-02 | `PokeApiMapperTest`, `PokeApiClientTest`, `PokemonControllerTest` (JSON shape) | bulbasaur with all fields |
| AC-03 | `PokemonTest` | `weightKg: 6.9` |
| AC-04 | `PokemonPageTest`, `PokemonControllerTest` | `totalPages: 68` |
| AC-05 | `PokemonControllerTest` (defaults) | — |
| AC-06 | `PokemonControllerTest` (`size=51`, `page=-1`, `size=0`, `page=abc`) | `400` with `errors` |
| AC-07 | `PokemonPageTest`, `ListPokemonUseCaseTest` | `page=9999` → empty `content` |
| AC-08 | `PokeApiClientTest` (5xx, timeout, invalid and empty body), `ListPokemonUseCaseTest`, `PokemonControllerTest` (`502`, `500`) | — |
| AC-09 | `PokeApiMapperTest` (null sprite and lists) | — |
| AC-10 | — (needs Redis; backlog item 1) | Cold page 1.2 s, cached page 27 ms; 21 keys with 24 h TTL |
| AC-11 | — (uses Spring's `LoggingCacheErrorHandler`, no own logic to test) | Redis stopped: `200`, health `UP`, `WARN` logs |

## Security

- Public route. Input validated with limits, so a client cannot ask for thousands of parallel calls.
- Error responses never contain stack traces, PokeAPI URLs or exception messages.

## Performance

- Parallel calls with virtual threads: a page of 20 costs about the time of the slowest call, not the sum.
- Redis cache with a long TTL: after the first request, a page is served without calling PokeAPI.
- Each Pokemon is cached on its own, so US02 and other pages reuse it.
- Without Redis, each cache read and write waits at most 1 s before falling back.
