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
  "type": "about:blank",
  "title": "Bad Gateway",
  "status": 502,
  "detail": "The Pokemon service is not available. Try again later.",
  "instance": "/api/v1/pokemon"
}
```

`400` responses for invalid parameters also include an `errors` list with the parameter name and message.

## Package structure (see ADR-001)

```text
com.poketechtest
├── domain/model/                 Pokemon, PokemonPage          (no framework code)
├── application/
│   ├── port/out/                 PokemonCatalogPort            (interface)
│   ├── exception/                ExternalServiceException
│   └── usecase/                  ListPokemonUseCase
├── infrastructure/
│   ├── pokeapi/                  PokeApiClient (adapter), PokeApi*Response DTOs, PokeApiMapper
│   └── config/                   PokeApiConfig (RestClient), CacheConfig, AsyncConfig
└── interfaces/rest/              PokemonController, PokemonSummaryResponse, PageResponse,
                                  PokemonRestMapper, GlobalExceptionHandler
```

## Flow

```text
PokemonController
  → ListPokemonUseCase.list(page, size)
      → PokemonCatalogPort.findPage(offset, limit)          [cached: pokeapi-pages]
      → for each name, in parallel:
          PokemonCatalogPort.findByName(name)               [cached: pokeapi-pokemon]
      → PokemonPage (original order)
  → PokemonRestMapper → PageResponse<PokemonSummaryResponse>
```

## Domain

- `Pokemon` (record): `id`, `name`, `spriteUrl`, `types`, `weightHectograms`, `moves`. Method `weightKg()` converts to kilograms (business rule AC-03). US02 will add fields to this record.
- `PokemonPage` (record): `items`, `page`, `size`, `totalElements`, and `totalPages()` computed from them.
- `PokemonCatalogPage` (record, in `application/port/out`): `names` of one PokeAPI page and the `totalCount`. It is the result of the list call of the port, not a domain concept.

## Application

- `PokemonCatalogPort` (out port):
  - `PokemonCatalogPage findPage(int offset, int limit)`
  - `Pokemon findByName(String name)`
- `ListPokemonUseCase`:
  - Computes `offset = page * size`.
  - Calls `findPage`, then `findByName` for each name with `CompletableFuture.supplyAsync(..., executor)` on a virtual-thread executor (TDR-002).
  - Joins the futures in the original order. If any future fails, it rethrows the cause (`ExternalServiceException` → `502`).
- `ExternalServiceException`: a business exception for "external catalog not available". The adapter throws it; the use case does not know about HTTP.

## Infrastructure

- `PokeApiConfig`: creates a `RestClient` with the base URL and the timeouts from properties (TDR-001).
- `PokeApiClient` implements `PokemonCatalogPort`:
  - `GET /pokemon?offset&limit` and `GET /pokemon/{name}`.
  - `@Cacheable("pokeapi-pages")` and `@Cacheable("pokeapi-pokemon")` on the two methods (TDR-003).
  - Converts `RestClientException` (4xx/5xx/timeout/invalid body) to `ExternalServiceException` and logs a `WARN` with the URL path and status. No payloads in the logs.
- PokeAPI DTOs as records with only the fields we use (`@JsonIgnoreProperties(ignoreUnknown = true)`).
- `PokeApiMapper` (MapStruct): DTO → domain, including the nested lists (`types[].type.name`, `moves[].move.name`, `sprites.front_default`).
- `CacheConfig`: `RedisCacheManager` with JSON values, key prefix and TTL from properties, and a `CacheErrorHandler` that logs a `WARN` and continues when Redis fails (AC-11).
- `AsyncConfig`: one `ExecutorService` bean with virtual threads, closed when the app stops.

## Interfaces (REST)

- `PokemonController`: `@Validated`, `@Min/@Max` on the parameters, OpenAPI annotations.
- `PokemonSummaryResponse`, `PageResponse<T>` (records).
- `PokemonRestMapper` (MapStruct): domain → response.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, extends `ResponseEntityExceptionHandler`):
  - `HandlerMethodValidationException`, `MethodArgumentTypeMismatchException` → `400` with `errors`.
  - `ExternalServiceException` → `502`.
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

Properties are bound to a `PokeApiProperties` record with `@ConfigurationProperties`.

## New dependencies

| Dependency | Reason |
|---|---|
| `spring-boot-starter-data-redis` | Redis connection for the cache. |
| `spring-boot-starter-cache` | Spring cache abstraction (`@Cacheable`). |
| `springdoc-openapi-starter-webmvc-ui` 3.1.1 | OpenAPI docs and Swagger UI (TDR-005). |

## Tests (unit only, decision 12)

| Test | Type | Covers |
|---|---|---|
| `PokemonTest` | Plain JUnit | AC-03 (kg conversion, rounding) |
| `PokemonPageTest` | Plain JUnit | AC-04, AC-07 (`totalPages`, empty page) |
| `ListPokemonUseCaseTest` | JUnit + Mockito (port mocked, real executor) | AC-01 (offset and order), AC-07, AC-08 (error propagation) |
| `PokeApiMapperTest` | Plain JUnit with the generated mapper | AC-02, AC-09 (null sprite) |
| `PokeApiClientTest` | `MockRestServiceServer` (no network) | Correct URLs, mapping, AC-08 (5xx and timeout → `ExternalServiceException`) |
| `PokemonControllerTest` | `@WebMvcTest` + `@MockitoBean` use case | AC-01, AC-02, AC-05, AC-06, AC-08 (status codes and JSON shape) |
| `CacheErrorHandlerTest` | Plain JUnit | AC-11 (errors are logged, not thrown) |

AC-10 (cache hit) needs a real Redis. It is checked manually with Docker Compose and stays in the backlog with the Testcontainers integration tests.

## Security

- Public route. Input validated with limits, so a client cannot ask for thousands of parallel calls.
- Error responses never contain stack traces, PokeAPI URLs or exception messages.

## Performance

- Parallel calls with virtual threads: a page of 20 costs about the time of the slowest call, not the sum.
- Redis cache with a long TTL: after the first request, a page is served without calling PokeAPI.
- Each Pokemon is cached on its own, so US02 and other pages reuse it.

## Files

Created: the classes listed above, `application.yml` (new properties), tests.
Modified: `pom.xml`, `.env.example` (optional PokeAPI variables), `docker-compose.yml` (none expected; `REDIS_HOST/PORT` already exist), `README.md` (endpoint and Swagger URL).
